package Application;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ATPManager;
import utils.BenchmarkHandler;
import utils.EvaluationProtocol;
import utils.NetworkManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class SecretDateHost {

    static Logger logger = LoggerFactory.getLogger(SecretDateHost.class);
    public static int myID;
    int maxBitLength;
    int numParties;
    static boolean logging;
    static boolean debug;
    boolean benchmark;

    public static final Integer benchmarkId = 2;

    public Network myNetwork;
    public SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce;
    public SpdzResourcePool myPool;
    public NetworkManager myNetworkManager;
    public ATPManager myManager;
    public List<ATPManager.ATPUnit> units;
    public EvaluationProtocol protocol;
    public PriceProtocol priceProtocol;

    public static void log(String info){
        if(debug || logging){
            logger.info(info);
        }
    }

    public void runProtocol(){

        BenchmarkHandler handler = BenchmarkHandler.getInstance();
        handler.startTimer(benchmarkId);
        handler.startNetwork(benchmarkId, myNetworkManager.getReceivedBytes());
        log("Setup aggregator");
        AggregateInputs aggregator = new AggregateInputs(this);
        log("Starting aggregator");
        ATPManager.instance.clearNetwork(myNetwork);
        mySce.runApplication(aggregator, myPool, myNetwork, Duration.ofMinutes(60));
        log("Starting Volume Checks");
        ATPManager.instance.clearNetwork(myNetwork);
        aggregator.checkVolumes(mySce, myPool, myNetwork, Duration.ofMinutes(60));
        log("Sorting Dates");
        ATPManager.instance.clearNetwork(myNetwork);
        Map<Integer, DRes<SInt>> dates = aggregator.sortByDate(mySce, myPool, myNetwork, Duration.ofMinutes(60));
        ATPManager.instance.clearNetwork(myNetwork);

        handler.endTimer(benchmarkId);
        handler.endNetwork(benchmarkId, myNetworkManager.getReceivedBytes());
        if(benchmark){
            PriceProtocolBenchmark priceProtocolBenchmark = new PriceProtocolBenchmark(myNetworkManager);
            log("Setup protocol benchmarking");
            priceProtocolBenchmark.initMPCParameters(mySce, myPool, myNetwork, Duration.ofMinutes(200));
            log("Evaluate Price for all SalesPositions for all protocols");
            Map<Integer, List<Boolean>> results = priceProtocolBenchmark.executeForAllPositions(aggregator.pricesTotal, dates,
                    aggregator.hostUnits, aggregator.volumesTotal, debug);

            logger.info("Results of the pricing");
            for(Map.Entry<Integer, List<Boolean>> entry : results.entrySet()){
                logger.info("Sales Position: " + entry.getKey() +
                        "\nResulted in: " + entry.getValue());
            }

        } else {

            log("Setup price Protocol " + protocol.toString());
            priceProtocol.initMPCParameters(mySce, myPool, myNetwork, Duration.ofMinutes(20));

            log("Evaluate Price for all SalesPositions");
            Map<Integer, Boolean> results = priceProtocol.executeForAllPositions(aggregator.pricesTotal, dates,
                    aggregator.hostUnits, aggregator.volumesTotal, debug);

            OpenProtocol openProtocol = new OpenProtocol(results, aggregator.unitListMap, priceProtocol.pricePerUnitMap);
            ATPManager.instance.clearNetwork(myNetwork);
            mySce.runApplication(openProtocol, myPool, myNetwork, Duration.ofMinutes(60));

            ATPManager.instance.exportResult(results, aggregator.unitListMap);
            logger.info("Results of the pricing");
            for(Map.Entry<Integer, Boolean> entry : results.entrySet()){
                logger.info("Sales Position: " + entry.getKey() +
                        "\nResulted in: " + entry.getValue());
            }
        }




    }



}
