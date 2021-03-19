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

    @Override
    public Integer checkProtocol(int myDate, Integer hostPrice){
        super.checkProtocol(myDate, hostPrice);
        int hostDate = debugDates.get(0);
        debugDates.remove(0);
        Collections.sort(debugDates);
        int clientDate = debugDates.get(0);

        if(SecretDateHost.myID == 0){
            int pricePremium = (hostDate - clientDate) * hostPrice;
            pricePremium /= hostDate;
            pricePremium += hostPrice;
            return pricePremium;
        }

        return 0;
    }

}
