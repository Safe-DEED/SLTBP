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

/**
 * Abstract parent class of pricing protocols.
 */
public abstract class PriceProtocol implements Application<BigInteger, ProtocolBuilderNumeric> {

    DRes<SInt> standardLeadTime, orderedLeadTime, priceHost, priceClient, resultPrice, clientVolume, resultEvaluation, pricePremium;
    DRes<BigInteger> price;

    // DEBUG Values
    DRes<BigInteger> standardLeadTimeOpen, orderedLeadTimeOpen, priceHostOpen, priceClientOpen, clientVolumeOpen;

    public  Integer benchmarkId;
    public Map<Integer, DRes<SInt>> pricePerUnitMap;

    boolean protocolInit = false;
    boolean mpcInit = false;
    boolean protocolFinished = false;
    boolean debug = false;

    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce;
    SpdzResourcePool pool;
    Network network;
    Duration duration;

    /**
     * Debug function used to perform the protocol in the clear. Opens all secret shared values necessary for the protocol.
     * @param seq protocol builder used to open the shared MPC values.
     */
    protected void openValues(ProtocolBuilderNumeric seq){
        if(debug){
            standardLeadTimeOpen = seq.numeric().open(standardLeadTime);
            orderedLeadTimeOpen  = seq.numeric().open(orderedLeadTime);
            priceHostOpen        = seq.numeric().open(priceHost);
            priceClientOpen      = seq.numeric().open(priceClient);
            clientVolumeOpen     = seq.numeric().open(clientVolume);
        }
    }

    /**
     * Initializes the protocol with the inputs of a single Sales Position
     * @param standardLeadTime The delivery date offered by the vendor
     * @param orderedLeadTime The delivery date of the clients (selected in AggregateInputs.sortByDate())
     * @param priceHost The price offered by the host
     * @param priceClient The combined price offered by the clients
     * @param clientVolume The combined volume requested by the clients
     * @param debug Whether computation should also be performed in the clear
     */
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

    /**
     * Initializes the FRESCO instances that are needed for MPC
     * @param Sce The secure computation engine running the computation
     * @param pool The Memory pool required for SPDZ preprocessing
     * @param network The network information for communication
     * @param duration The maximum network timeout
     */
    public void initMPCParameters(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                                  SpdzResourcePool pool, Network network, Duration duration){
        this.Sce = Sce;
        this.pool = pool;
        this.network = network;
        this.duration = duration;
        this.mpcInit = true;
    }

    /**
     * Executed the given pricing evaluation protocol for all sales positions (SP).
     * @param clientPrices connects SPs with combined client prices
     * @param orderedDates connects SPs with a date
     * @param hostUnits connects each SP with the associated ATPUnit of the host
     * @param clientVolumes connects SPs with the combined client volumes
     * @param debug states whether computation should also be performed in the clear. UNSAFE
     * @return map connecting SPs with boolean result of evaluation
     * @apiNote Stores price premium of each evaluation in the pricePerUnitMap
     */
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
        pricePerUnitMap = new HashMap<>();
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
            boolean result;
            if (standardHigherOrder <= 0) {
                SecretDateHost.log("order date is bigger or equal than standard -> standard is considered");
                MultApplication multApplication = new MultApplication(priceHost, volumeClient);
                ATPManager.instance.clearNetwork(network);
                DRes<SInt> totalPrice = Sce.runApplication(multApplication, pool, network, duration);
                pricePremium = priceHost;
                int res = comparator.compare(priceClient, totalPrice);
                result = res > -1;
            } else {
                initProtocol(standardDate, orderDate, priceHost, priceClient, volumeClient, debug);
                SecretDateHost.log("Start protocol for SP: " + salesPosition);
                ATPManager.instance.clearNetwork(network);
                BigInteger res = Sce.runApplication(this, pool, network, duration);
                result = res.equals(BigInteger.ONE);
            }
            results.put(salesPosition, result);
            SecretDateHost.log("result for " + salesPosition + " is " + result);
            if(result){
                SecretDateHost.log("putting pricePremium for " + salesPosition);
                pricePerUnitMap.put(salesPosition, pricePremium);
            }
            if(debug){
                SecretDateHost.log("Final result check yields: " + checkResult());
            }
        }

        return results;
    }

    /**
     * Required by interface
     */
    @Override
    public void close(){

    }

    /**
     * Helper function returning a string containing all opened values necessary for the computation. works only in debug mode.
     * @return either a pretty string of contents in debug mode or the class name in production mode
     */
    public String stringify(){
        if(debug){
            return "\nThis price protocol(" + this.getClass().toString() + ") has the following input:\nstandard Lead: " + standardLeadTimeOpen.out() + "\nordered Lead: " + orderedLeadTimeOpen.out() + "\n priceHost: " + priceHostOpen.out() + "\npriceClient: " + priceClientOpen.out() + "\nclientVolume:" +  clientVolumeOpen.out() + "\nprice expected:" + price.out();
        }
        return this.getClass().toString();
    }

    /**
     * Debug function to compare MPC computation to clear implementation
     * @return boolean result
     */
    public abstract boolean checkResult();

}
