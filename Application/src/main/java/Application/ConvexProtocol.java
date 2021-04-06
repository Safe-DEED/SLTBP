package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;


import java.math.BigInteger;

public class ConvexProtocol extends PriceProtocol{

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;
        return builder.seq(seq -> {
            SecretDateHost.logger.info("Starting Convex Price Protocol");
            Numeric numeric = seq.numeric();
            if(debug){
                openValues(seq);
            }
            AdvancedNumeric advancedNumeric = AdvancedNumeric.using(seq);
            long power_two = 65536; // 2^16, we estimate ln(2^16) = 11
            long ln_power_two = 11;
            DRes<SInt> known_mul = numeric.mult(power_two, standardLeadTime);
            DRes<SInt> div = advancedNumeric.div(known_mul, orderedLeadTime);
            DRes<SInt> log = advancedNumeric.log(div, seq.getBasicNumericContext().getMaxBitLength());
            DRes<SInt> sub = numeric.sub(log, ln_power_two);
            DRes<SInt> half = advancedNumeric.div(sub, 2);
            resultPrice = numeric.mult(half, clientVolume);
            return () -> null;
        }).seq((seq, nil) -> {
            if(debug){
                price = seq.numeric().open(resultPrice, 1);
            }
            resultEvaluation = Comparison.using(seq).compareLEQ(resultPrice, priceClient);
            protocolFinished = true;
            return () -> null;
        }).seq((seq, nil) -> seq.numeric().open(resultEvaluation));
    }

    @Override
    public boolean checkResult() {
        return false;
    }
}
