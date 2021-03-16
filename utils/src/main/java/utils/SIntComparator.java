package utils;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Comparator;

public class SIntComparator implements Comparator<DRes<SInt>>, Application<BigInteger, ProtocolBuilderNumeric> {

    private DRes<SInt> o1, o2, aC, bC;
    private DRes<BigInteger> a, b;
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce;
    SpdzResourcePool pool;
    Network network;
    Duration duration;

    public SIntComparator(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce, SpdzResourcePool pool, Network network, Duration duration){
        this.Sce = Sce;
        this.pool = pool;
        this.network = network;
        this.duration = duration;
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {

        return builder.seq(seq -> {
            aC = Comparison.using(seq).compareLEQ(o1, o2);
            bC = Comparison.using(seq).compareLEQ(o2, o1);
            return () -> null;
        }).seq((seq, nil) -> {
            a = seq.numeric().open(aC);
            b = seq.numeric().open(bC);
            return () -> null;
        });
    }

    @Override
    public int compare(DRes<SInt> o1, DRes<SInt> o2) {
        this.o1 = o1;
        this.o2 = o2;
        Sce.runApplication(this, pool, network, duration);
        BigInteger o1Small = a.out();
        BigInteger o2Small = b.out();

        if(o1Small.equals(BigInteger.ZERO)){
            return 1;
            // O1 greater O2
        }
        if(o2Small.equals(BigInteger.ZERO)){
            return -1;
            // O2 greater O1
        }

        return 0;
        // O1 == O2
    }

    @Override
    public void close() {
        Sce.shutdownSCE();
    }
}
