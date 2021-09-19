package utils;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;

/**
 * A helper application to quickly multiply two secret shared values
 */
public class MultApplication  implements Application<SInt, ProtocolBuilderNumeric> {

    DRes<SInt> a, b;

    /**
     * Init multiplication protocol
     * @param a left-hand operator
     * @param b right-hand operator
     */
    public MultApplication(DRes<SInt> a, DRes<SInt> b){
        this.a = a;
        this.b = b;
    }

    /**
     * Compute a * b and return result as SInt
     * @param builder used to create MPC native protocol
     * @return secret share of a * b
     */
    @Override
    public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
        return builder.seq(seq -> seq.numeric().mult(a, b));
    }

    @Override
    public void close() {
    }
}
