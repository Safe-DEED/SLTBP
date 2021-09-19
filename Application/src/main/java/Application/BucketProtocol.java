package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the Bucket Protocol as a pricing evaluation
 */
public class BucketProtocol extends PriceProtocol{

    DRes<SInt> percent, comparison1, comparison2, comparison3, scalesClientPrice;
    DRes<BigInteger> openComp1, openComp2, openComp3;
    Map<Integer, Integer> bucketToPremiumMap = new HashMap<>();
    Integer premium;


    /**
     * Checks whether an integer value is either 1 or zero
     * @param value Integer to check
     * @return true if value is 0 or 1, false otherwise
     */
    private boolean isNotBool(long value){
        return value != 1 && value != 0;
    }

    /**
     * Constructor. Initializes the price premium map. The values are taken directly from IFX.
     */
    public BucketProtocol(){
        super();
        benchmarkId = 6;
        bucketToPremiumMap.put(7, 100);
        bucketToPremiumMap.put(6, 102);
        bucketToPremiumMap.put(5, 105);
        bucketToPremiumMap.put(4, 110);
        bucketToPremiumMap.put(3, 120);
        bucketToPremiumMap.put(2, 140);
        bucketToPremiumMap.put(1, 180);
        bucketToPremiumMap.put(0, 190);
    }

    /**
     * MPC computation implementation. Here the relative delivery speedup is calculated and the according price Premium
     * is taken from the bucket
     * @param builder the protocol builder used to construct the MPC blocks
     * @return A bigInteger of either 1 or 0 (true or false), whether the deal has succeeded
     */
    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {

        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;

        return builder.seq(seq -> {
            SecretDateHost.log("Starting bucket price finder");
            if(debug){
                openValues(seq);
            }
            Numeric numeric = seq.numeric();
            DRes<SInt> sub = numeric.sub(standardLeadTime, orderedLeadTime);
            DRes<SInt> scale = numeric.mult(100, sub);
            percent = AdvancedNumeric.using(seq).div(scale, standardLeadTime);
            return  null;
        }).seq((seq, nil) -> {
            comparison1 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(50));
            return null;
        }).seq((seq, nil) -> {
            openComp1 = seq.numeric().open(comparison1);
            return null;
        }).seq((seq, nil) -> {
            if(openComp1.out().equals(BigInteger.ONE)){
                comparison2 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(20));
            } else {
                comparison2 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(80));
            }
            return null;
        }).seq((seq, nil) -> {
            openComp2 = seq.numeric().open(comparison2);
            return null;
        }).seq((seq, nil) -> {
            if(openComp1.out().equals(BigInteger.ONE)){
                if(openComp2.out().equals(BigInteger.ONE)){
                    comparison3 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(10));
                } else {
                    comparison3 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(35));
                }
            } else {
                if(openComp2.out().equals(BigInteger.ONE)){
                    comparison3 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(65));
                } else {
                    comparison3 = Comparison.using(seq).compareLEQ(percent, seq.numeric().known(90));
                }
            }
            return null;
        }).seq((seq, nil) -> {
            openComp3 = seq.numeric().open(comparison3);
            return null;
        }).seq((seq, nil) -> {
            long comp1 = openComp1.out().longValue();
            long comp2 = openComp2.out().longValue();
            long comp3 = openComp3.out().longValue();
            if(isNotBool(comp1) || isNotBool(comp2) || isNotBool(comp3)){
                throw new RuntimeException("Error in comparison or opening of values");
            }
            int mapAccess = (int) (4 * comp1 + 2 * comp2 + comp3);
            premium = bucketToPremiumMap.get(mapAccess);
            SecretDateHost.log("map category: " + mapAccess + " -> " + premium);
            DRes<SInt> singlePrice = seq.numeric().mult(premium, priceHost);
            pricePremium = AdvancedNumeric.using(seq).div(singlePrice, 100);
            resultPrice = seq.numeric().mult(singlePrice, clientVolume);
            scalesClientPrice = seq.numeric().mult(100, priceClient);
            return null;
        }).seq((seq, nil) -> {
            resultEvaluation = Comparison.using(seq).compareLEQ(resultPrice, scalesClientPrice);
            return null;
        }).seq((seq, nil) -> {
            if(debug){
                price = seq.numeric().open(resultPrice);
            }
            protocolFinished = true;
            return seq.numeric().open(resultEvaluation);
        });
    }


    /**
     * Debug function computing the MPC computations in plain
     * @return bool signaling whether the plain computation has the same result as the MPC computation
     */
    @Override
    public boolean checkResult(){
        if(debug){
            SecretDateHost.log("checking result\n\n");
            long standard = standardLeadTimeOpen.out().longValue();
            long ordered = orderedLeadTimeOpen.out().longValue();
            long sub = standard - ordered;
            double div = 100 * ((double) sub / standard);
            int pricePremium;
            if(div <= 10){
                pricePremium = 0;
            } else if(div <= 20){
                pricePremium = 2;
            } else if(div <= 35){
                pricePremium = 5;
            } else if(div <= 50){
                pricePremium = 10;
            } else if(div <= 65){
                pricePremium = 20;
            } else if(div <= 80){
                pricePremium = 40;
            } else if(div <= 90){
                pricePremium = 80;
            } else if(div <= 100){
                pricePremium = 90;
            } else{
                return false;
            }
            pricePremium += 100;
            long totalPrice = pricePremium * priceHostOpen.out().longValue() * clientVolumeOpen.out().longValue();
            SecretDateHost.log(super.stringify());
            SecretDateHost.log("Correct result: " + totalPrice);
            SecretDateHost.log("Actual result: " + this.price.out());
            SecretDateHost.log("\n\n");
            return totalPrice == this.price.out().longValue();
        }
        return false;
    }
}
