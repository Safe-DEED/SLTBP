package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import utils.ATPManager;

import java.math.BigInteger;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public abstract class PriceProtocol implements Application<BigInteger, ProtocolBuilderNumeric> {

    DRes<SInt> standardLeadTime, orderedLeadTime, priceHost, priceClient, resultPrice, clientVolume;

    boolean protocolInit = false;
    boolean mpcInit = false;

    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce;
    SpdzResourcePool pool;
    Network network;
    Duration duration;

    public void initProtocol(DRes<SInt> standardLeadTime, DRes<SInt> orderedLeadTime,
                             DRes<SInt> priceHost, DRes<SInt> priceClient, DRes<SInt> clientVolume){
        this.standardLeadTime = standardLeadTime;
        this.orderedLeadTime = orderedLeadTime;
        this.priceClient = priceClient;
        this.priceHost = priceHost;
        this.resultPrice = null;
        this.clientVolume = clientVolume;
        protocolInit = true;
    }

    public void initMPCParameters(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                                  SpdzResourcePool pool, Network network, Duration duration){
        this.Sce = Sce;
        this.pool = pool;
        this.network = network;
        this.duration = duration;
        this.mpcInit = true;
    }

    public Map<Integer, Boolean> executeForAllPositions(Map<Integer, DRes<SInt>> clientPrices,
                                                        Map<Integer, DRes<SInt>> orderedDates,
                                                        Map<Integer, ATPManager.ATPUnit> hostUnits,
                                                        Map<Integer, DRes<SInt>> clientVolumes) {
        if(clientPrices.size() != orderedDates.size() || orderedDates.size() != hostUnits.size()){
            throw new IllegalArgumentException("Maps for Protocol evaluation have to be of same size");
        }
        if(!mpcInit){
            throw new IllegalStateException("Call to protocol before initialization");
        }

        Map<Integer, Boolean> results = new HashMap<>();

        for(Map.Entry<Integer, ATPManager.ATPUnit> hostEntry : hostUnits.entrySet()){
            int salesPosition = hostEntry.getKey();
            SecretDateHost.logger.info("Setup protocol for SP: " + salesPosition);
            ATPManager.ATPUnit hostUnit = hostEntry.getValue();
            DRes<SInt> orderDate = orderedDates.getOrDefault(salesPosition, null);
            DRes<SInt> priceClient = clientPrices.getOrDefault(salesPosition, null);
            DRes<SInt> volumeClient = clientVolumes.getOrDefault(salesPosition, null);
            DRes<SInt> priceHost = hostUnit.closedPrice;
            DRes<SInt> standardDate = hostUnit.closedDate;
            if(orderDate == null || priceClient == null || priceHost == null || standardDate == null || volumeClient == null){
                throw new IllegalArgumentException("Input to price protocol has to be pre-shared and cannot be null");
            }
            initProtocol(standardDate, orderDate, priceHost, priceClient, volumeClient);
            SecretDateHost.logger.info("Start protocol for SP: " + salesPosition);
            BigInteger res = Sce.runApplication(this, pool, network, duration);
            results.put(salesPosition, res.equals(BigInteger.ONE));
        }

        return results;
    }

}
