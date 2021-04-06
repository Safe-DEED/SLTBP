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

public class BucketProtocol extends PriceProtocol{

    DRes<SInt> percent, comparison1, comparison2, comparison3;
    DRes<BigInteger> openComp1, openComp2, openComp3;
    Map<Integer, Integer> bucketToPremiumMap = new HashMap<>();

    private boolean isNotBool(long value){
        return value != 1 && value != 0;
    }

    @Override
    public boolean checkResult() {
        return false;
    }

    public BucketProtocol(){
        super();
        bucketToPremiumMap.put(7, 100);
        bucketToPremiumMap.put(6, 102);
        bucketToPremiumMap.put(5, 105);
        bucketToPremiumMap.put(4, 110);
        bucketToPremiumMap.put(3, 120);
        bucketToPremiumMap.put(2, 140);
        bucketToPremiumMap.put(1, 180);
        bucketToPremiumMap.put(0, 190);
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {


        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;

        return builder.seq(seq -> {
            SecretDateHost.logger.info("Starting bucket price finder");
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
            int pricePremium = bucketToPremiumMap.get(mapAccess);
            DRes<SInt> singlePrice = seq.numeric().mult(pricePremium, priceHost);
            resultPrice = seq.numeric().mult(singlePrice, clientVolume);
            return null;
        }).seq((seq, nil) -> {
            resultEvaluation = Comparison.using(seq).compareLEQ(resultPrice, priceClient);
            return null;
        }).seq((seq, nil) -> {
            if(debug){
                price = seq.numeric().open(resultPrice);
            }
            protocolFinished = true;
            return seq.numeric().open(resultEvaluation);
        });
    }
}
