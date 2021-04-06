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

public class AggregateInputs implements Application<BigInteger, ProtocolBuilderNumeric> {

    private final List<ATPManager.ATPUnit> myUnits;
    private final List<Integer> salesPositions;
    private final List<Integer> partyList;
    private final SecretDateHost secretDateHost;
    private final Map<Integer, List<ATPManager.ATPUnit>> unitListMap;

    public Map<Integer, DRes<SInt>> pricesTotal = new HashMap<>();
    public Map<Integer, DRes<SInt>> volumesTotal= new HashMap<>();
    public Map<Integer, ATPManager.ATPUnit> hostUnits = new HashMap<>();
    public Map<Integer, List<DRes<SInt>>> datesAll = new HashMap<>();

    private boolean error;

    public static class SortByPosition implements Comparator<ATPManager.ATPUnit>{
        public int compare(ATPManager.ATPUnit a, ATPManager.ATPUnit b){
            return a.salesPosition - b.salesPosition;
        }
    }

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
            if(id == secretDateHost.myID){

                int amount = salesPositions.size();
                if (error) {
                    ATPManager.instance.broadcastInt(0, secretDateHost.myNetwork);
                    continue;
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
                    continue;
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
        SecretDateHost.logger.info("List before sort: " + myUnits);
        myUnits.sort(new SortByPosition());
        SecretDateHost.logger.info("List after sort: " + myUnits);
        ATPManager.instance.clearNetwork(secretDateHost.myNetwork);
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(error) {
            return null;
        }


        return builder.seq(seq -> {
            Numeric numeric = seq.numeric();
            SecretDateHost.logger.info("Input values to aggregator");
            for (int i = 0; i < myUnits.size(); ++i) {
                int salesPos = salesPositions.get(i);
                SecretDateHost.logger.info("gathering input of SP "+ salesPos);
                ATPManager.ATPUnit myUnit = myUnits.get(i);
                ATPManager.ATPUnit currentUnit;
                List<ATPManager.ATPUnit> currentUnitList = new ArrayList<>();

                for (int id : partyList) {
                    SecretDateHost.logger.info("gathering input of Party "+ id);
                    if (secretDateHost.myID == id) {
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
                SecretDateHost.logger.info("Map: " + unitListMap);
            }
            return () -> null;
        }).seq((seq, nil) -> {
            SecretDateHost.logger.info("Summing up values");
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

    public Map<Integer, DRes<SInt>> sortByDate(SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> Sce,
                                                     SpdzResourcePool pool, Network network, Duration duration){
        Map<Integer, DRes<SInt>> result = new HashMap<>();
        SIntComparator comparator = new SIntComparator(Sce, pool, network, duration);
        SecretDateHost.logger.info("Sorting salesPosition by date");
        for(Map.Entry<Integer, List<DRes<SInt>>> entry : datesAll.entrySet()){
            List<DRes<SInt>> current_dates = entry.getValue();
            DRes<SInt> min = Collections.min(current_dates, comparator);
            result.put(entry.getKey(), min); // At the moment lowest date is returned
        }
        return result;
    }

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
            SecretDateHost.logger.info("deleted " + pos + " from lists");
        }


    }

    @Override
    public void close() {

    }
}
