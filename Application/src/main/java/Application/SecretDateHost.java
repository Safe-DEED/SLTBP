package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ATPManager;
import utils.BenchmarkHandler;
import utils.NetworkManager;
import utils.SIntComparator;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
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

    public enum EvaluationProtocol{
        LINEAR,
        CONVEX,
        CONCAVE,
        BUCKET;
    }

    public void runProtocol(){

        BenchmarkHandler handler = BenchmarkHandler.getInstance();
        handler.startTimer(benchmarkId);

        log("Setup aggregator");
        AggregateInputs aggregator = new AggregateInputs(this);
        log("Starting aggregator");
        ATPManager.instance.clearNetwork(myNetwork);
        mySce.runApplication(aggregator, myPool, myNetwork, Duration.ofMinutes(10));
        log("Starting Volume Checks");
        ATPManager.instance.clearNetwork(myNetwork);
        aggregator.checkVolumes(mySce, myPool, myNetwork, Duration.ofMinutes(30));
        log("Sorting Dates");
        ATPManager.instance.clearNetwork(myNetwork);
        Map<Integer, DRes<SInt>> dates = aggregator.sortByDate(mySce, myPool, myNetwork, Duration.ofMinutes(15));
        ATPManager.instance.clearNetwork(myNetwork);

        handler.endTimer(benchmarkId);

        if(benchmark){
            PriceProtocolBenchmark priceProtocolBenchmark = new PriceProtocolBenchmark();
            log("Setup protocol benchmarking");
            priceProtocolBenchmark.initMPCParameters(mySce, myPool, myNetwork, Duration.ofMinutes(20));
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

            logger.info("Results of the pricing");
            for(Map.Entry<Integer, Boolean> entry : results.entrySet()){
                logger.info("Sales Position: " + entry.getKey() +
                        "\nResulted in: " + entry.getValue());
            }
        }




    }



}
