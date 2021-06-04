package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.gates.SpdzMultProtocol;
import utils.ATPManager;
import utils.MultApplication;
import utils.SIntComparator;

import java.math.BigInteger;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public abstract class PriceProtocol implements Application<BigInteger, ProtocolBuilderNumeric> {

    DRes<SInt> standardLeadTime, orderedLeadTime, priceHost, priceClient, resultPrice, clientVolume, resultEvaluation;
    DRes<BigInteger> price;

    // DEBUG Values
    DRes<BigInteger> standardLeadTimeOpen, orderedLeadTimeOpen, priceHostOpen, priceClientOpen, clientVolumeOpen;

    public  Integer benchmarkId;

    boolean protocolInit = false;
    boolean mpcInit = false;
    boolean protocolFinished = false;
    boolean debug = false;

    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce;
    SpdzResourcePool pool;
    Network network;
    Duration duration;

    protected void openValues(ProtocolBuilderNumeric seq){
        if(debug){
            standardLeadTimeOpen = seq.numeric().open(standardLeadTime);
            orderedLeadTimeOpen  = seq.numeric().open(orderedLeadTime);
            priceHostOpen        = seq.numeric().open(priceHost);
            priceClientOpen      = seq.numeric().open(priceClient);
            clientVolumeOpen     = seq.numeric().open(clientVolume);
        }
    }

    public void initProtocol(DRes<SInt> standardLeadTime, DRes<SInt> orderedLeadTime,
                             DRes<SInt> priceHost, DRes<SInt> priceClient, DRes<SInt> clientVolume, boolean debug){
        this.standardLeadTime = standardLeadTime;
        this.orderedLeadTime = orderedLeadTime;
        this.priceClient = priceClient;
        this.priceHost = priceHost;
        this.resultPrice = null;
        this.clientVolume = clientVolume;
        this.debug = debug;
        protocolInit = true;
        protocolFinished = false;
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
                                                        Map<Integer, DRes<SInt>> clientVolumes,
                                                        boolean debug) {
        if(clientPrices.size() != orderedDates.size() || orderedDates.size() != hostUnits.size()){
            throw new IllegalArgumentException("Maps for Protocol evaluation have to be of same size");
        }
        if(!mpcInit){
            throw new IllegalStateException("Call to protocol before initialization");
        }

        Map<Integer, Boolean> results = new HashMap<>();
        SIntComparator comparator = new SIntComparator(Sce, pool, network, duration);
        for(Map.Entry<Integer, ATPManager.ATPUnit> hostEntry : hostUnits.entrySet()) {
            int salesPosition = hostEntry.getKey();
            SecretDateHost.log("Setup protocol for SP: " + salesPosition);
            ATPManager.ATPUnit hostUnit = hostEntry.getValue();
            DRes<SInt> orderDate = orderedDates.getOrDefault(salesPosition, null);
            DRes<SInt> priceClient = clientPrices.getOrDefault(salesPosition, null);
            DRes<SInt> volumeClient = clientVolumes.getOrDefault(salesPosition, null);
            DRes<SInt> priceHost = hostUnit.closedPrice;
            DRes<SInt> standardDate = hostUnit.closedDate;
            if (orderDate == null || priceClient == null || priceHost == null || standardDate == null || volumeClient == null) {
                throw new IllegalArgumentException("Input to price protocol has to be pre-shared and cannot be null");
            }
            int standardHigherOrder = comparator.compare(standardDate, orderDate);
            if (standardHigherOrder <= 0) {
                SecretDateHost.log("order date is bigger or equal than standard -> standard is considered");
                MultApplication multApplication = new MultApplication(priceHost, clientVolume);
                ATPManager.instance.clearNetwork(network);
                DRes<SInt> totalPrice = Sce.runApplication(multApplication, pool, network, duration);
                int res = comparator.compare(priceClient, totalPrice);
                results.put(salesPosition, res > -1);
            } else {
                initProtocol(standardDate, orderDate, priceHost, priceClient, volumeClient, debug);
                SecretDateHost.log("Start protocol for SP: " + salesPosition);
                ATPManager.instance.clearNetwork(network);
                BigInteger res = Sce.runApplication(this, pool, network, duration);
                boolean result = res.equals(BigInteger.ONE);
                results.put(salesPosition, result);
                if(result){

                }
                if(debug){
                    SecretDateHost.log("Final result check yields: " + checkResult());
                }
            }
        }

        return results;
    }

    @Override
    public void close(){

    }

    public String stringify(){
        if(debug){
            return "\nThis price protocol(" + this.getClass().toString() + ") has the following input:\nstandard Lead: " + standardLeadTimeOpen.out() + "\nordered Lead: " + orderedLeadTimeOpen.out() + "\n priceHost: " + priceHostOpen.out() + "\npriceClient: " + priceClientOpen.out() + "\nclientVolume:" +  clientVolumeOpen.out() + "\nprice expected:" + price.out();
        }
        return this.getClass().toString();
    }

    public abstract boolean checkResult();

}
