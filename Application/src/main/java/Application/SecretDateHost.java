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
    int myID;
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

    public enum EvaluationProtocol{
        LINEAR,
        CONVEX,
        CONCAVE,
        BUCKET
    }

    public void runProtocol(){
        VolumeCheck volumeCheck;
        BigInteger result;

        for (ATPManager.ATPUnit unit: units) {
            volumeCheck = new VolumeCheck(this, unit.amount);
            result = mySce.runApplication(volumeCheck, myPool, myNetwork, Duration.ofMinutes(5));
            logger.info("The result of the volume check is: " + result);
        }
    }



}
