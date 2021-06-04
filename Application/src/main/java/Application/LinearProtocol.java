package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;

import java.math.BigInteger;
import java.util.Collections;

public class LinearProtocol extends PriceProtocol{

    public LinearProtocol(){
        benchmarkId = 3;
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {

        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;
        return builder.seq(seq -> {
            SecretDateHost.log("Starting linear price Computation");
            if(debug){
                openValues(seq);
            }
            Numeric numeric = seq.numeric();
            DRes<SInt> sub = numeric.sub(standardLeadTime, orderedLeadTime);
            DRes<SInt> mul = numeric.mult(sub, priceHost);
            DRes<SInt> div = AdvancedNumeric.using(seq).div(mul, standardLeadTime);
            pricePremium = numeric.add(priceHost, div);
            resultPrice = numeric.mult(pricePremium, clientVolume);
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

    @Override
    public boolean checkResult() {
        if(!protocolFinished){
            return false;
        }
        if(debug){
            SecretDateHost.log("checking result\n\n\n");
            BigInteger sub = standardLeadTimeOpen.out().subtract(orderedLeadTimeOpen.out());
            BigInteger mul = sub.multiply(priceHostOpen.out());
            BigInteger div = mul.divide(standardLeadTimeOpen.out());
            BigInteger add = div.add(priceHostOpen.out());
            BigInteger result = add.multiply(clientVolumeOpen.out());
            SecretDateHost.log(super.stringify());
            return result.equals(price.out());
        }

        return false;
    }

}
