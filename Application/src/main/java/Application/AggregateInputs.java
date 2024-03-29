package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import utils.ATPManager;
import utils.SIntComparator;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

/**
 * The aggregate input class combines the inputs of all customers. It is an implementation of the FRESCO Application interface
 */
public class AggregateInputs implements Application<BigInteger, ProtocolBuilderNumeric> {

    private final List<ATPManager.ATPUnit> myUnits;
    private final List<Integer> salesPositions;
    private final List<Integer> partyList;
    private final SecretDateHost secretDateHost;
    public final Map<Integer, List<ATPManager.ATPUnit>> unitListMap;

    public Map<Integer, DRes<SInt>> pricesTotal = new HashMap<>();
    public Map<Integer, DRes<SInt>> volumesTotal= new HashMap<>();
    public Map<Integer, ATPManager.ATPUnit> hostUnits = new HashMap<>();
    public Map<Integer, List<DRes<SInt>>> datesAll = new HashMap<>();

    private boolean error;

    /**
     * Comparator class used to order ATPUnits with respect to their salesPosition
     */
    public static class SortByPosition implements Comparator<ATPManager.ATPUnit>{
        public int compare(ATPManager.ATPUnit a, ATPManager.ATPUnit b){
            return a.salesPosition - b.salesPosition;
        }
    }

    /**
     * Constructor of Aggregate Inputs. Checks uniqueness of sales Positions and whether all participants share the same SPs.
     * @param secretDateHost The secretDateHost instance containing all the inputs
     * @throws IllegalArgumentException if there is some error with the SPs
     */
    public AggregateInputs(SecretDateHost secretDateHost) throws IllegalArgumentException{
        error = false;
        partyList = new ArrayList<>();
        unitListMap = new HashMap<>();
        this.myUnits = secretDateHost.units;
        this.secretDateHost = secretDateHost;
        salesPositions = new ArrayList<>();

        for(ATPManager.ATPUnit unit : myUnits){
            if(salesPositions.contains(unit.salesPosition)){
                throw new IllegalArgumentException("Duplicate SalesPosition in input:" + unit.salesPosition);
            }
            salesPositions.add(unit.salesPosition);
        }
        Collections.sort(salesPositions);
        for(Map.Entry<Integer, Party> entry : secretDateHost.myNetworkManager.getParties().entrySet()){
            int id = entry.getKey();
            partyList.add(id);
            if(id == SecretDateHost.myID){

                int amount = salesPositions.size();
                if (error) {
                    ATPManager.instance.broadcastInt(0, secretDateHost.myNetwork);
                } else {
                    ATPManager.instance.broadcastInt(amount, secretDateHost.myNetwork);
                }
                for (int sPos : salesPositions){
                    ATPManager.instance.broadcastInt(sPos, secretDateHost.myNetwork);
                }

            } else {
                int amount = ATPManager.instance.receiveInt(id, secretDateHost.myNetwork);

                if(amount == 0){
                    error = true;
                }
                if(amount != salesPositions.size()){
                    error = true;
                }

                for(int i = 0; i < amount; ++i){
                    int current = ATPManager.instance.receiveInt(id, secretDateHost.myNetwork);
                    if(!salesPositions.contains(current)){
                        error = true;
                    }
                }
            }
            if(error){
                throw new IllegalArgumentException("Mismatch in SalesPosition List:\n" + salesPositions);
            }
        }
        myUnits.sort(new SortByPosition());
        SecretDateHost.log("List after sort: " + myUnits);
    }

    /**
     * The MPC protocol aggregating the inputs from all clients. The aggregated inputs are stored in the current
     * instance of AggregateInputs for later use
     * @param builder the protocol builder is passed by the secure computation engine in the run call
     * @return null
     */
    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(error) {
            return null;
        }

        return builder.seq(seq -> {
            Numeric numeric = seq.numeric();
            SecretDateHost.log("Input values to aggregator");
            for (int i = 0; i < myUnits.size(); ++i) {
                int salesPos = salesPositions.get(i);
                SecretDateHost.log("gathering input of SP "+ salesPos);
                ATPManager.ATPUnit myUnit = myUnits.get(i);
                ATPManager.ATPUnit currentUnit;
                List<ATPManager.ATPUnit> currentUnitList = new ArrayList<>();

                for (int id : partyList) {
                    SecretDateHost.log("gathering input of Party "+ id);
                    if (SecretDateHost.myID == id) {
                        myUnit.closedDate = numeric.input(myUnit.date, id);
                        myUnit.closedAmount = numeric.input(myUnit.amount, id);
                        myUnit.closedPrice = numeric.input(myUnit.price, id);
                        currentUnit = myUnit;
                    } else {
                        currentUnit = new ATPManager.ATPUnit(id, null, null, null, salesPos);
                        currentUnit.closedDate = numeric.input(null, id);
                        currentUnit.closedAmount = numeric.input(null, id);
                        currentUnit.closedPrice = numeric.input(null, id);
                    }
                    currentUnitList.add(currentUnit);
                }
                unitListMap.put(salesPos, currentUnitList);
                SecretDateHost.log("Map: " + unitListMap);
            }
            return () -> null;
        }).seq((seq, nil) -> {
            SecretDateHost.log("Summing up values");
            for(Map.Entry<Integer, List<ATPManager.ATPUnit>> entry : unitListMap.entrySet()){
                List<ATPManager.ATPUnit> currentList = entry.getValue();
                int salesPos = entry.getKey();
                List<DRes<SInt>> dates = new ArrayList<>();
                List<DRes<SInt>> prices = new ArrayList<>();
                List<DRes<SInt>> volumes = new ArrayList<>();
                for(ATPManager.ATPUnit unit : currentList){
                    if(unit.id != 1){
                        prices.add(unit.closedPrice);
                        volumes.add(unit.closedAmount);
                        dates.add(unit.closedDate);
                    } else{
                        hostUnits.put(salesPos, unit);
                    }
                }
                pricesTotal.put(salesPos, AdvancedNumeric.using(seq).sum(prices));
                volumesTotal.put(salesPos, AdvancedNumeric.using(seq).sum(volumes));
                datesAll.put(salesPos, dates);
            }
            return () -> null;
        });
    }

    /**
     * Creates a map from all SalesPositions to the lowest corresponding date. It uses MPC comparison between the customers dates
     * @param Sce the secure computation engine used for the MPC comparison
     * @param pool the resource pool required by the MPC comparison
     * @param network the network with which the MPC protocol works
     * @param duration the maximum time to wait for the networking
     * @return the map from SalesPositions to secret shared dates
     */
    public Map<Integer, DRes<SInt>> sortByDate(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                                                     SpdzResourcePool pool, Network network, Duration duration){
        Map<Integer, DRes<SInt>> result = new HashMap<>();
        SIntComparator comparator = new SIntComparator(Sce, pool, network, duration);
        SecretDateHost.log("Sorting salesPosition by date");
        for(Map.Entry<Integer, List<DRes<SInt>>> entry : datesAll.entrySet()){
            List<DRes<SInt>> current_dates = entry.getValue();
            DRes<SInt> min = Collections.min(current_dates, comparator);
            //DRes<SInt> min = current_dates.get(0);
            result.put(entry.getKey(), min); // At the moment lowest date is returned
        }
        return result;
    }

    /**
     * Checks for all SalesPositions whether the vendor amount is higher than the sum of the clients amounts. Uses
     * MPC powered comparison. Sales Positions, where the clients amounts are higher, are discarded.
     * @param Sce Secure Computation engine required by MPC
     * @param pool Resource pool required for SPDZ preprocessing
     * @param network stores networking information for communication
     * @param duration the maximum time to wait for the networking
     */
    public void checkVolumes(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                             SpdzResourcePool pool, Network network, Duration duration){
        SIntComparator comparator = new SIntComparator(Sce, pool, network, duration);
        List<Integer> toRemove = new ArrayList<>();
        for(Map.Entry<Integer, ATPManager.ATPUnit> hostEntry : hostUnits.entrySet()){
            int salesPos = hostEntry.getKey();
            DRes<SInt> hostVolume = hostEntry.getValue().closedAmount;
            DRes<SInt> clientVolume = volumesTotal.getOrDefault(salesPos, null);
            if(clientVolume == null){
                throw new IllegalArgumentException("call to check Volumes failed. No client sum available. Did you call" +
                        "checkVolume before aggregation?");
            }
            int comp = comparator.compare(hostVolume, clientVolume);
            if(comp < 0){
                toRemove.add(salesPos);
            }
        }
        for(int pos : toRemove){
            hostUnits.remove(pos);
            volumesTotal.remove(pos);
            datesAll.remove(pos);
            pricesTotal.remove(pos);
            SecretDateHost.log("deleted " + pos + " from lists");
        }


    }

    @Override
    public void close() {

    }
}
