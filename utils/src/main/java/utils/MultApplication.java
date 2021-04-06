package utils;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;

public class MultApplication  implements Application<SInt, ProtocolBuilderNumeric> {

    DRes<SInt> a, b;

    public MultApplication(DRes<SInt> a, DRes<SInt> b){
        this.a = a;
        this.b = b;
    }

    @Override
    public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
        return builder.seq(seq -> seq.numeric().mult(a, b));
    }

    @Override
    public void close() {
    }
}
