package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;


import java.math.BigInteger;

/**
 * Implementation of the Concave Protocol as a pricing evaluation
 */
public class ConvexProtocol extends PriceProtocol{

    DRes<SInt>  premiumLimit, powerOLT, powerSDT;
    DRes<BigInteger> isOverflow, pricePremiumOpen, powerOLTOpen, powerSDTOpen;


    /**
     * Constructor.
     */
    public ConvexProtocol(){
        benchmarkId = 5;
    }

    /**
     * Implementation of the MPC computation. The price premium is calculated as a convex function of the delivery speedup.
     * @param builder instance used to create the MPC computation blocks.
     * @return Boolean Bigint (either 0 or 1) depending on success of the deal.
     */
    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;
        return builder.seq(seq -> {
            SecretDateHost.log("Starting Convex Price Protocol");
            if(debug){
                openValues(seq);
            }
            AdvancedNumeric advancedNumeric = AdvancedNumeric.using(seq);
            int bitLen = seq.getBasicNumericContext().getMaxBitLength();
            powerOLT = advancedNumeric.exp(orderedLeadTime, 10);
            powerSDT = advancedNumeric.exp(standardLeadTime, 10);
            powerOLT = advancedNumeric.log(powerOLT, bitLen);
            powerSDT = advancedNumeric.log(powerSDT, bitLen);
            pricePremium = seq.numeric().sub(powerSDT, powerOLT);
            // Divide by 2  to approximate log10
            pricePremium = AdvancedNumeric.using(seq).div(pricePremium, 2);
            return null;
        }).seq((seq, nil) -> {
            premiumLimit = Comparison.using(seq).compareLEQ(pricePremium, seq.numeric().known(20));
            if(debug)
            {
                powerOLTOpen = seq.numeric().open(powerOLT);
                powerSDTOpen = seq.numeric().open(powerSDT);
            }
            return null;
        }).seq((seq, nil) -> {
            isOverflow = seq.numeric().open(premiumLimit);
            return null;
        }).seq((seq, nil) -> {
            if(debug){
                SecretDateHost.log("powerOLT: " + powerOLTOpen.out() + "\npowerSDT: "+ powerSDTOpen.out());
            }
            if(isOverflow.out().equals(BigInteger.ZERO)){
                pricePremium = seq.numeric().known(40);
                SecretDateHost.log("scaled down!");
            } else {
                pricePremium = seq.numeric().add(20, pricePremium);
            }
            pricePremium = seq.numeric().mult(pricePremium, priceHost);
            // Divide by 10 to compensate previous exponentiation
            // Divide by 2  to apply formula from deliverable 7.5
            pricePremium = AdvancedNumeric.using(seq).div(pricePremium, 20);
            resultPrice = seq.numeric().mult(pricePremium, clientVolume);
            return null;
        }).seq((seq, nil) -> {
            if(debug){
                price = seq.numeric().open(resultPrice);
                pricePremiumOpen = seq.numeric().open(pricePremium);
            }
            resultEvaluation = Comparison.using(seq).compareLEQ(resultPrice, priceClient);
            protocolFinished = true;
            if(pricePremium == null){
                throw new RuntimeException("price premium null");
            }
            return null;
        }).seq((seq, nil) -> seq.numeric().open(resultEvaluation));
    }


    /**
     * Checking whether the MPC computation works. The result is compared with a margin of error, to account for rounding issues.
     * @return boolean result
     */
    @Override
    public boolean checkResult() {
        if(debug) {
            SecretDateHost.log("checking result\n\n\n");
            long standard = standardLeadTimeOpen.out().longValue();
            long ordered = orderedLeadTimeOpen.out().longValue();
            long clientVol = clientVolumeOpen.out().longValue();
            double div = (double)standard / ordered;
            double log = Math.log(div) / Math.log(10);
            log /= 2;
            log = (log > 1) ? 2 : log + 1;
            log *= priceHostOpen.out().longValue();
            long result = (long)(log * clientVol);
            long actual = price.out().longValue();
            SecretDateHost.log(super.stringify());
            SecretDateHost.log("Correct result: " + result);
            SecretDateHost.log("Actual result:" + actual);
            return (Math.abs(result - actual) < result * 0.1);
        }
        return false;
    }
}
