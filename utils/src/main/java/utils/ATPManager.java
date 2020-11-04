package utils;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.SInt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * The ATPManager holds all the logic used to work with ATPUnits, which are the core objects for this use case.
 * In other words: the ATPManager contains all the computation logic which is shared between the customers and infineon.
 * This class could be separated into application logic and ATPUnit modifier (i.e. ATPManager) at some point
 */
public class ATPManager {

    public Map<Integer, List<ATPUnit>> units;
    public List<ATPUnit> unitList;
    private static Logger log = LoggerFactory.getLogger(ATPManager.class);
    private final int myID;
    public Map<Integer, Integer> amountMap;
    public int noOfPlayers;
    public int maxBitLength;
    public Network orderNetwork, dealNetwork;
    public List<DRes<SInt>> ATPMinCost; // only neeeded when comparison is public
    public List<DRes<SInt>> ATPCost;
    public List<DRes<SInt>> ATPLeftOver;
    public List<DRes<SInt>> ATPVolumes; // only neeeded when comparison is made open at host
    public List<DRes<BigInteger>> DEBUGMinCost;
    public List<DRes<BigInteger>> DEBUGCost;
    public List<DRes<BigInteger>> DEBUGLeftOver;
    public List<DRes<BigInteger>> DEBUGVolumes;
    public DRes<BigInteger> cond1, cond2;
    public List<DRes<SInt>> clientVolumeSum, clientPriceVSum;
    public List<DRes<BigInteger>> VolSum, PriSum;
    public List<BigInteger> hostVol, hostPrice;
    public static ATPManager instance;
    public Function priceCalculation;
    public Integer selectedDeal;


    /**
     * As the ATPManager has to work for the entire computation and the context in which its funtions are called changes,
     * the ATPManager needs to have a lot of state. It also functions as a container class for the Applications. Hence, a lot
     * of Lists and Maps are created.
     * @param id The user id of the Party to which this ATPManager instance belongs.
     */
    private ATPManager(int id){
        this.ATPMinCost  = new ArrayList<>();
        this.ATPCost     = new ArrayList<>();
        this.ATPLeftOver = new ArrayList<>();
        this.ATPVolumes  = new ArrayList<>();
        this.unitList    = new ArrayList<>();
        this.units       = new HashMap<>();
        this.amountMap   = new HashMap<>();
        this.clientPriceVSum = new ArrayList<>();
        this.clientVolumeSum = new ArrayList<>();
        this.hostVol = new ArrayList<>();
        this.hostPrice = new ArrayList<>();
        this.myID        = id;
    }

    /**
     * ATPManager is designed as a singleton
     * @param id id of current party executing the protocol
     * @return instance of ATPManager
     */
    public static ATPManager getInstance(int id){
        if(ATPManager.instance == null){
            ATPManager.instance = new ATPManager(id);
        } else if(instance.myID != id){
            log.info("Wrong id ATPManager called!");
        }
        return ATPManager.instance;
    }


    /**
     * Setter for the priceCalculation function
     * @param priceCalculation Evaluator of the final protocol step
     */
    public void setPriceCalculation(Function priceCalculation) {
        this.priceCalculation = priceCalculation;
    }

    /**
     * A very basic network function, which expects an integer from a certain party in the network async.
     * @param id the party from which to expect an input
     * @param network The network to receive from
     * @return the received integer.
     */
    private int receiveInt(int id, Network network){
        return ByteBuffer.wrap(network.receive(id)).getInt();
    }

    /**
     * Sending an integer to every party in the network.
     * @param Int The integer which is to send to everyone
     * @param network The network to broadcast to
     */
    private void broadcastInt(int Int, Network network){
        network.sendToAll(ByteBuffer.allocate(Integer.BYTES).putInt(Int).array());
    }

    /**
     * The amount of ATPUnits is shared over the network this is then stored in the amount map.
     * @param amount of ATPUnits which the owner of this manager wants to have
     */
    public void createAmountMap(Integer amount){
        for(int i = 1; i < orderNetwork.getNoOfParties() + 1; i++){
            if(i == myID){
                broadcastInt(amount, orderNetwork);
            } else {
                amountMap.put(i, receiveInt(i, orderNetwork));
            }
        }
    }

    /**
     * Auxiliary function of the sumWithDate method. Adds entries to the three lists
     * @param minEntry The minimum cost per unit times the current amount requested for this date
     * @param costEntry The current offered price for the amount requested for this date
     * @param leftEntry The amount left for infineon if the current amount requested is sold - should never be less than zero
     * @param volume The current volume for this entry
     */
    private void updateSumLists(DRes<SInt> minEntry, DRes<SInt> costEntry, DRes<SInt> leftEntry, DRes<SInt> volume){
        ATPMinCost.add(minEntry);
        ATPCost.add(costEntry);     // compare each entry of atp cost to match ATPMin Cost
        ATPLeftOver.add(leftEntry); //
        ATPVolumes.add(volume);
    }

    /**
     * This funtion opens the three Lists used in the sumWithDate function. This is for
     * Debug purposes only!
     * @param protocolBuilderNumeric the protocol builder used to add the open protocol to the protocol chain
     */
    public void CreateDebugLists(ProtocolBuilderNumeric protocolBuilderNumeric, int id){
        DEBUGCost = new ArrayList<>();
        DEBUGLeftOver = new ArrayList<>();
        DEBUGMinCost  = new ArrayList<>();
        DEBUGVolumes  = new ArrayList<>();
        //log.info("opening sumLists!");
        for(int i = 0; i < ATPLeftOver.size(); i++){
            DEBUGCost.add(protocolBuilderNumeric.numeric().open(ATPCost.get(i), id));
            DEBUGLeftOver.add(protocolBuilderNumeric.numeric().open(ATPLeftOver.get(i), id));
            DEBUGMinCost.add(protocolBuilderNumeric.numeric().open(ATPMinCost.get(i), id));
            DEBUGVolumes.add(protocolBuilderNumeric.numeric().open(ATPVolumes.get(i), id));
        }
    }

    /**
     * print out all the values from the debug lists. Can not be called in the same application step as the CreateDebugLists method.
     *
     */
    public void printDebug(){
        if(myID != 1){
            return;
        }
        log.info("CostList: ");
        for (DRes<BigInteger> bigInteger : DEBUGCost) {
            System.out.println(bigInteger.out());
        }
//        log.info("LeftOver: ");
//        for (DRes<BigInteger> bigInteger : DEBUGLeftOver) {
//            System.out.println(bigInteger.out());
//        }
        log.info("MinCost : ");
        for (DRes<BigInteger> bigInteger : DEBUGMinCost) {
            System.out.println(bigInteger.out());
        }
        log.info("Volumes : ");
        for (DRes<BigInteger> bigInteger : DEBUGVolumes){
            System.out.println(bigInteger.out());
        }
    }

    /**
     * For each date at which infineon offers units does the following:
     * First all the requested volumes are summed up, and all the prices given for these volumes are also summed up.
     * Then the difference in requested and provided volume is stored and added to the provided volume for the next date.
     * Also the requested volume times the minimum price is stored for later comparison.
     * @param protocolBuilderNumeric the protocol builder adds all the numeric operations for later execution
     */
    public void sumWithDate(ProtocolBuilderNumeric protocolBuilderNumeric)  {
        DRes<SInt> leftOver = null;
        DRes<SInt> current = null;
        DRes<SInt> currentMinCost = null;
        DRes<SInt> currentCost = null;
        //log.info("sum with date!");
        for(ATPUnit unit : unitList){
            if(unit.id == 1){
                if(current != null){
                    if(leftOver == null){ // The host unit has to be first!
                        log.error("ATP cannot be orderd before date: " + unit.date);
                    }
                    leftOver = protocolBuilderNumeric.numeric().sub(leftOver, current);
                    updateSumLists(protocolBuilderNumeric.numeric().mult(currentMinCost, current), currentCost, leftOver, current);
                    current = null;
                    currentCost = null;
                }
                leftOver = (leftOver == null) ? unit.closedAmount : protocolBuilderNumeric.numeric().add(leftOver, unit.closedAmount);
                currentMinCost = unit.closedPrice;

            } else {
                current = (current == null) ? unit.closedAmount : protocolBuilderNumeric.numeric().add(current, unit.closedAmount);
                currentCost = (currentCost == null) ? unit.closedPrice : protocolBuilderNumeric.numeric().add(unit.closedPrice, currentCost);
            }
        }
        if(current != null){
            leftOver = protocolBuilderNumeric.numeric().sub(leftOver, current);
            updateSumLists(protocolBuilderNumeric.numeric().mult(currentMinCost, current), currentCost, leftOver, current);
        }
    }

    /**
     * Sum up the client amounts and prices for a single date.
     * @param pBN The protocol builder used to evaluate the addition
     */
    public void sumIndividualDate(ProtocolBuilderNumeric pBN){
        Numeric numeric = pBN.numeric();
        DRes<SInt> clientVolumeSum = null;
        DRes<SInt> clientPriceVSum = null;

        int date = -1;
        for(ATPUnit unit : unitList){
            if(unit.date < date){
                throw new RuntimeException("Date iteration failed at " + unit.date + " by unit of id: " + unit.id);
            } if(unit.date > date){
                date = unit.date;
                if(clientPriceVSum != null){
                    this.clientVolumeSum.add(clientVolumeSum);
                    this.clientPriceVSum.add(clientPriceVSum);
                    clientVolumeSum = null;
                    clientPriceVSum = null;
                }
            } if(unit.id == 1){
                hostVol.add(unit.amount);
                hostPrice.add(unit.price);
                continue;
            }
            clientVolumeSum = (clientVolumeSum == null) ? unit.closedAmount : numeric.add(clientVolumeSum, unit.closedAmount);
            clientPriceVSum = (clientPriceVSum == null) ? unit.closedPrice :  numeric.add(clientPriceVSum, unit.closedPrice);
        }

        if(clientPriceVSum != null){
            this.clientPriceVSum.add(clientPriceVSum);
            this.clientVolumeSum.add(clientVolumeSum);
        }

    }

    /**
     * Opens the summed up volumes and prices to the host.
     * @param protocolBuilderNumeric the protocol builder used to evaluate the open call
     */
    public void openPriceAndVolSum(ProtocolBuilderNumeric protocolBuilderNumeric){
        Numeric numeric = protocolBuilderNumeric.numeric();
        VolSum = new ArrayList<>();
        PriSum = new ArrayList<>();

        for(int i = 0; i < this.clientVolumeSum.size(); i++){
            VolSum.add(numeric.open(clientVolumeSum.get(i), 1));
            PriSum.add(numeric.open(clientPriceVSum.get(i), 1));
        }
    }

    /**
     * Auxilliary function to convert an ATPUnit to a JSON object
     * @param unit Unit to be transformed to json
     * @param host Defines whether the host or the client makes the call
     * @return JSONObject representing an ATPUnit such as in ATPUnits.json
     */
    @SuppressWarnings("unchecked")
    private JSONObject unitToJSON(ATPUnit unit, Boolean host){
        JSONObject object = new JSONObject();
        object.put("id", String.valueOf(unit.id));
        if(host){
            ATPUnit myUnit = units.get(1).stream().filter(u -> u.date.equals(unit.date)).findAny().orElse(unit);
            object.put("amount", String.valueOf(unit.openedAmount.out()));
            object.put("price", String.valueOf(unit.openedPrice.out()));
            object.put("Sales Position", String.valueOf(myUnit.salesPosition));
        } else{
            object.put("amount", String.valueOf(unit.amount));
            object.put("price", String.valueOf(unit.price));
            object.put("Sales Position", String.valueOf(unit.salesPosition));
        }

        JSONObject object1 = new JSONObject();
        object1.put("unit", object);
        return object1;
    }

    /**
     * Exporting the result to a json file. we select the data which is accepted and export all atpunits of this date
     * to a json file
     * @param selectedDeal selected date for atpUnit selection
     */
    @SuppressWarnings("unchecked")
    public void exportResult(Integer selectedDeal){
        JSONArray unitsList = new JSONArray();
        for (ATPUnit unit : unitList) {
            if(myID == 1){
                if(unit.id != 1 && unit.date.equals(selectedDeal)){
                    unitsList.add(unitToJSON(unit, true));
                }
            } else{
                if(unit.id == myID && unit.date.equals(selectedDeal)){
                    unitsList.add(unitToJSON(unit, false));
                    System.out.println(unitList);
                }
            }
        }
        //Write JSON file
        try (FileWriter file = new FileWriter("accepted_order.json")) {
            if(unitsList.isEmpty()){
                JSONObject fail = new JSONObject();
                fail.put("deal", "failed");
                unitsList.add(fail);
            }
            file.write(unitsList.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Applies the pricing function on the evaluated mpc circuit
     * @param protocolBuilderNumeric The protocol builder used to perform operations using MPC
     */
    public OpenStatus OpenEvaluation(ProtocolBuilderNumeric protocolBuilderNumeric) {
        if(priceCalculation != null) {
            try{
                priceCalculation.apply(protocolBuilderNumeric);
                log.info("The deal succeeded!");
                return OpenStatus.valueOf(selectedDeal);
            }
            catch (Exception e){
                if(!e.getMessage().equals("")){
                    log.info(e.getMessage());
                }
                log.info("The deal failed!");
                return OpenStatus.valueOf(selectedDeal);
            }
        }
        return OpenStatus.NO_EVAL_FUNCTION;
    }


    /**
     * In this function the two comparisons are made for each of the provided dates:
     * Firstly, whether infineon offers a higher amount than requested - using the stored leftover values.
     * Secondly, whether the price offered by the clients is higher than the minimum price set by infineon.
     * NOTE: this function could be executed in the same step as sumWithDate, it was merely separated to see which function
     * takes the most time (it is the comparison)
     * @param protocolBuilderNumeric the protocol builder used to append the mpc protocols for later execution
     */
    public void EvalConditions(ProtocolBuilderNumeric protocolBuilderNumeric){
        CreateDebugLists(protocolBuilderNumeric, 1);
        DRes<SInt> leftOverSign = null;
        DRes<SInt> lOS, costDiff, cOS;
        DRes<SInt> costSign = null;
        for (int i = 0; i < ATPLeftOver.size(); i++) {
            lOS = protocolBuilderNumeric.comparison().compareLEQ(protocolBuilderNumeric.numeric().known(BigInteger.ZERO), ATPLeftOver.get(i));
            leftOverSign = (leftOverSign == null) ? lOS : protocolBuilderNumeric.numeric().mult(leftOverSign, lOS);
            DEBUGLeftOver.add(protocolBuilderNumeric.numeric().open(leftOverSign));
            costDiff = protocolBuilderNumeric.numeric().sub(ATPCost.get(i), ATPMinCost.get(i));
            cOS = protocolBuilderNumeric.comparison().compareLEQ(protocolBuilderNumeric.numeric().known(BigInteger.ZERO), costDiff);
            costSign = (costSign == null) ? cOS : protocolBuilderNumeric.numeric().mult(cOS, costSign);
        }
        this.cond1 = protocolBuilderNumeric.numeric().open(leftOverSign);
        this.cond2 = protocolBuilderNumeric.numeric().open(costSign);

    }


    /**
     * Checks whether for all given dates all conditions are true
     * @return is the deal possible (true/false)
     */
    public Integer isDealPossible(OpenStatus status){
        if(status != null){
            broadcastInt(status.getValue(), dealNetwork);
        } else{
            status = OpenStatus.valueOf(receiveInt(1, dealNetwork));
        }
        switch (status){
            case FAIL:
                return -1;
            case NO_EVAL_FUNCTION:
                throw new RuntimeException("No Evaluation function was provided");
            default:
                return status.getValue();
        }
        //if(true) { return true; }
        //return (this.cond1.out().equals(BigInteger.ONE) && this.cond2.out().equals(BigInteger.ONE));
    }


    /**
     * if the deal is possible, opens the list of ATPUnits to the Host (i.e., Infineon)
     * @param protocolBuilderNumeric the protocol builder used to add the open protocol for later execution
     */
    public void openList(ProtocolBuilderNumeric protocolBuilderNumeric, Integer selectedDeal){
        if(selectedDeal > 0){
            for(ATPUnit unit : unitList){
                if(unit.id == 1){
                    continue;
                } if(unit.date.equals(selectedDeal)){
                    open(unit, 1, protocolBuilderNumeric);
                }
            }
        }
    }

    /**
     * When creating an ATPUnit, this function creates the secretly shared values, and distributes them.
     * @param unit the unit to be shared or to receive shares for
     * @param protocolBuilderNumeric the protocol builder to add the input protocol
     */
    private void input(ATPUnit unit, ProtocolBuilderNumeric protocolBuilderNumeric){
        unit.closedAmount = protocolBuilderNumeric.numeric().input(unit.amount, unit.id);
        unit.closedPrice  = protocolBuilderNumeric.numeric().input(unit.price, unit.id);
        if(unit.date == null){
            unit.date = receiveInt(unit.id, orderNetwork);
        } else {
            broadcastInt(unit.date, orderNetwork);
        }
   }

    /**
     * Adding an existing unit to the unitlist and secret sharing its price and amount
     * @param unit the unit to be added and secretly distributed
     * @param numeric the protocol builder
     */
   public void createUnit(ATPUnit unit, ProtocolBuilderNumeric numeric){
        input(unit, numeric);
        units.computeIfAbsent(unit.id, (i) -> new ArrayList<>()).add(unit);
        unitList.add(unit);
   }

    /**
     * Creating a new ATP unit from scratch, if not the owner, then secret shares will be expected of the owner.
     * @param unitID The id of the party this unit belongs to.
     * @param date   The date, when this unit is requested (as client), or when it is available (host/infineon)
     * @param amount The amount requested or null if it belongs to someone else
     * @param price  The price offered or null if it belongs to someone else
     * @param protocolBuilderNumeric the protocol builder used in the input function
     */
   public void createUnit(int unitID, Integer date, BigInteger amount, BigInteger price, ProtocolBuilderNumeric protocolBuilderNumeric){
        ATPUnit unit = new ATPUnit(unitID, date, amount, price);
        createUnit(unit, protocolBuilderNumeric);
   }

    /**
     * This function opens the secret values of a given ATPUnit
     * @param unit the unit to be opened
     * @param id the identification of the party, the unit has to be opened for. E.g. id=1, the unit will be opened for infineon
     * @param protocolBuilderNumeric the protocol builder used to open the values.
     */
   public void open(ATPUnit unit, int id, ProtocolBuilderNumeric protocolBuilderNumeric){
        unit.openedAmount = protocolBuilderNumeric.numeric().open(unit.closedAmount, id);
        unit.openedPrice  =  protocolBuilderNumeric.numeric().open(unit.closedPrice, id);
        unit.opened = true;
   }

    /**
     * The ATPUnit class, an abstraction of an order and an offer. implements the comparable interface, to
     * easily sort the unitlist using collections.sort().
     */
    public static class ATPUnit implements Comparable<ATPUnit>{
        final int id;
        BigInteger amount;
        DRes<SInt> closedAmount;
        DRes<BigInteger> openedAmount;
        Integer date;
        Integer salesPosition;
        Integer RLZ;
        Integer OLT;
        BigInteger price;
        DRes<SInt> closedPrice;
        DRes<BigInteger> openedPrice;
        private Boolean opened = false;

        /**
         * The constructor of the ATPUnit. initializes the unit with the given params.
         * if the date is not given, the date is expected of the owner of this unit.
         * Should at some point be made private ...
         * @param id The owner of this unit - does not have to be the same as the owner of the manager
         * @param date the date of this unit, either the requested or provided
         * @param amount the amount requested/provided
         * @param price either the minimum price per unit or the the total price of this batch
         */
        public ATPUnit(int id, Integer date, BigInteger amount, BigInteger price){
            this.id = id;
            this.amount = amount;
            this.price = price;
            this.date = date;
        }


        public ATPUnit(int id, Integer date, BigInteger amount, BigInteger price, int salesPosition, int olt, int rlz){
            this(id, date, amount, price);
            this.salesPosition = salesPosition;
            this.OLT = olt;
            this.RLZ = rlz;
        }


        /**
         * This class is serializable for debug reasons
         * @return the serialized version of this object
         */
        public String toString(){
            if(opened){
                return "\nThis unit of: " + id+ " contains: " + openedAmount.out() +", with date: " + date + ", and price: " + openedPrice.out() + "\n";
            }
            return "\nThis unit of: " + id+ " contains: " + amount +", with date: " + date + ", and price: " + price + "\n";
        }

        /**
         * The compareTo method implementing the comparable interface. This unit is compared according to its date.
         * @param o the unit this unit has to be compared to
         * @return negative number if this unit is smaller, 0 if the units are equal, pos if this unit is bigger
         */
        @Override
        public int compareTo(ATPUnit o) {
            if(this.date.equals(o.date)){
                if(this.id == 1){
                    return -1;
                } else if (o.id == 1){
                    return 1;
                }
            }
            return this.date - o.date;
        }
    }

    /**
     * The ATP Units of the Host(Infineon) are parsed from a JSON file
     * @param unit the current JSON Object, which should contain a unit
     * @param manager The ATPManager instance, used to create a new ATP Unit
     * @return the newly created ATPUnit object with the parsed contents
     */
    public static ATPUnit parseATPUnit(JSONObject unit, ATPManager manager)
    {

        //Get atp object within list
        JSONObject atpUnit = (JSONObject) unit.get("unit");

        //Get the price
        BigInteger price = new BigInteger((String) atpUnit.get("price"));

        //Get the amount
        BigInteger amount = new BigInteger((String) atpUnit.get("amount"));

        int sp = Integer.parseInt((String) atpUnit.get("Sales Position"));

        if(manager.myID != 1){
            price = price.multiply(amount);
        }
        //Get date
        int date = Integer.parseInt((String) atpUnit.get("date"));
        return new ATPUnit(manager.myID, date, amount, price, sp, 0, 0);
    }

    /**
     * Serialization of the entire ATPManager object. Printing out every ATPUnit it contains.
     * @return Serialized string.
     */
    public String toString(){
        StringBuilder returnString = new StringBuilder("\n");
        for(Map.Entry<Integer, List<ATPUnit>> entry : units.entrySet()){
            returnString.append("The units of ID: ");
            returnString.append(entry.getKey());
            returnString.append("\n");
            for (ATPUnit unit : entry.getValue()){
                returnString.append(unit.toString());
            }
        }
        return returnString.toString();
    }

    /**
     * Status enum of evaluation function - signifies result of the protocol
     */
    public enum OpenStatus{
        NO_EVAL_FUNCTION(0),
        FAIL(-1),
        SUCCESS1(1),
        SUCCESS2(2),
        SUCCESS3(3);
        private final Integer value;
        private static final Map<Integer, OpenStatus> map = new HashMap<>();
        private OpenStatus(int val){
            value = val;
        }
        static {
            for (OpenStatus status: OpenStatus.values()){
                map.put(status.value, status);
            }
        }
        public static OpenStatus valueOf(Integer status){
            return map.get(status);
        }
        public int getValue(){
            return value;
        }

    }

    /**
     * Interface for the evaluation function of the protocol
     */
    @FunctionalInterface
    public interface Function{
        ATPManager manager = null;
        void apply(ProtocolBuilderNumeric protocolBuilderNumeric);
    }

}
