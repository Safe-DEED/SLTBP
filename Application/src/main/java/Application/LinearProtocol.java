package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;

import java.math.BigInteger;

public class LinearProtocol extends PriceProtocol{


    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(!protocolInit){
            throw new IllegalStateException("Running Price evaluation before protocol init");
        }
        protocolInit = false;
        return builder.seq(seq -> {
            SecretDateHost.logger.info("Starting linear price Computation");
            Numeric numeric = seq.numeric();
            DRes<SInt> sub = numeric.sub(standardLeadTime, orderedLeadTime);
            DRes<SInt> mul = numeric.mult(sub, priceHost);
            DRes<SInt> div = AdvancedNumeric.using(seq).div(mul, standardLeadTime);
            DRes<SInt> add = numeric.add(priceHost, div);
            resultPrice = numeric.mult(add, clientVolume);
            return () -> null;
        }).seq((seq, nil) -> {
            resultPrice = Comparison.using(seq).compareLEQ(resultPrice, priceClient);
            return () -> null;
        }).seq((seq, nil) -> seq.numeric().open(resultPrice));
    }

    @Override
    public void close() {

    }


}
