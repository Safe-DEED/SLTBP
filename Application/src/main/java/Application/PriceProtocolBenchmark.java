package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import utils.ATPManager;
import utils.BenchmarkHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceProtocolBenchmark {

    private final List<PriceProtocol> protocolList;


    public PriceProtocolBenchmark(){
        protocolList = new ArrayList<>();
        protocolList.add(new LinearProtocol());
        protocolList.add(new ConcaveProtocol());
        protocolList.add(new ConvexProtocol());
        protocolList.add(new BucketProtocol());
    }

    public void initMPCParameters(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                                  SpdzResourcePool pool, Network network, Duration duration){
        for(PriceProtocol protocol : protocolList){
            protocol.initMPCParameters(Sce, pool, network, duration);
        }
    }

    public Map<Integer, List<Boolean>> executeForAllPositions(Map<Integer, DRes<SInt>> clientPrices,
                                                              Map<Integer, DRes<SInt>> orderedDates,
                                                              Map<Integer, ATPManager.ATPUnit> hostUnits,
                                                              Map<Integer, DRes<SInt>> clientVolumes,
                                                              boolean debug){
        Map<Integer, List<Boolean>> returnValues = new HashMap<>();
        BenchmarkHandler handler = BenchmarkHandler.getInstance();
        for(PriceProtocol protocol: protocolList){
            handler.startTimer(protocol.benchmarkId);

            Map<Integer, Boolean> result = protocol.executeForAllPositions(clientPrices, orderedDates, hostUnits, clientVolumes, debug);

            for(Map.Entry<Integer, Boolean> entry : result.entrySet()){
                returnValues.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
            ATPManager.instance.clearNetwork(protocol.network);
            handler.endTimer(protocol.benchmarkId);
        }

        return returnValues;
    }

}
