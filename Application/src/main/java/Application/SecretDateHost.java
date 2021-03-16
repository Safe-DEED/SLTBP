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
import utils.NetworkManager;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecretDateHost {

    static Logger logger = LoggerFactory.getLogger(SecretDateHost.class);
    public int myID;
    int maxBitLength;
    int numParties;
    int myVolume;
    int minPrice;
    int myDate;
    boolean logging;

    public Network myNetwork;
    public SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce;
    public SpdzResourcePool myPool;
    public NetworkManager myNetworkManager;
    public ATPManager myManager;
    public List<ATPManager.ATPUnit> units;
    public EvaluationProtocol protocol;
    public PriceProtocol priceProtocol;

    public enum EvaluationProtocol{
        LINEAR,
        CONVEX,
        CONCAVE,
        BUCKET;
    }

    public void runProtocol(){

        logger.info("Setup aggregator");
        AggregateInputs aggregator = new AggregateInputs(this);
        logger.info("Starting aggregator");
        mySce.runApplication(aggregator, myPool, myNetwork, Duration.ofMinutes(5));
        logger.info("Starting Volume Checks");
        aggregator.checkVolumes(mySce, myPool, myNetwork, Duration.ofMinutes(2));
        logger.info("Sorting Dates");
        Map<Integer, DRes<SInt>> dates = aggregator.sortByDate(mySce, myPool, myNetwork, Duration.ofMinutes(2));

        logger.info("Setup price Protocol " + protocol.toString());
        priceProtocol.initMPCParameters(mySce, myPool, myNetwork, Duration.ofMinutes(5));

        logger.info("Evaluate Price for all SalesPositions");
        Map<Integer, Boolean> results = priceProtocol.executeForAllPositions(aggregator.pricesTotal, dates,
                                        aggregator.hostUnits, aggregator.volumesTotal);

        logger.info("Results of the pricing");
        for(Map.Entry<Integer, Boolean> entry : results.entrySet()){
            logger.info("Sales Position: " + entry.getKey() +
                    "\nResulted in: " + entry.getValue());
        }


    }



}
