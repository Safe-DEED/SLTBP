package utils;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static utils.ATPManager.parseATPUnit;


/**
 * The abstract builder class to instantiate the application objects. This builder takes care of the boiler plate code
 * which is required to setup the framework. It also handles the use case specific input, these two parts could
 * be separated at some point..
 * @param <ObjectT> The Object type the Builder extension wants to implement (either client or host)
 */
public abstract class MPCBuilder<ObjectT> extends FRESCOBuilder<ObjectT>{
    protected int myVolume;
    protected int myPrice;
    protected int myDate;
    protected int amount;
    protected List<ATPManager.ATPUnit> units;
    protected ATPManager myManager;


    /**
     * Creating the builder following the builder pattern.
     * @param logging whether logging is activated during the computation
     */
    public MPCBuilder(boolean logging){
        super(logging);
    }

    /**
     * Setting the minimum price per unit or the total price for the requested batch
     * @param price the price to set
     * @return this
     */
    public MPCBuilder<ObjectT> withPrice(int price){
        this.myPrice = price;
        return this;
    }

    /**
     * Requested date or date of availability
     * @param date the date to set
     * @return this
     */
    public MPCBuilder<ObjectT> withDate(int date){
        this.myDate = date;
        return this;
    }

    /**
     * Setting the ATP units as a List and not individually
     * @param units the list of parsed ATP Units used
     * @return this - used by build pattern
     */
    public MPCBuilder<ObjectT> withUnits(JSONArray units){
        this.units = new ArrayList<>();
        if(units == null){
            return this;
        }
        for(Object unit : units){
            ATPManager.ATPUnit cUnit = parseATPUnit((JSONObject) unit, myManager);
            this.units.add(cUnit);
        }
        return this;
    }

    /**
     * Setting
     * @param volume the requested amount or the provided amount
     * @param amount the number of atp units provided - if > 1 then volume and price are ignored
     * @return this
     */
    public MPCBuilder<ObjectT> withVolume(int volume, int amount){
        this.myVolume = volume;
        this.amount = amount;
        return this;
    }

    /**
     * Initializing the resourcePool Object required by the framework. MASCOT has to be used as a preprocessingStrategy
     * to achieve active security
     * @param strategy the PreprocessingStrategy used
     * @param modBitLength the bitLength of the modulus (128 bit is recommended)
     * @param obliviousTransferProtocol Define whether secure NAOR Pinkas or Dummy shall be used
     * @return this
     * @throws ParseException thrown when preprocessingStrategy is unknown
     */
    @Override
    public MPCBuilder<ObjectT> withResourcePool(PreprocessingStrategy strategy, int modBitLength, CmdLineParser.obliviousTransferProtocol obliviousTransferProtocol) throws ParseException {
        super.withResourcePool(strategy, modBitLength, obliviousTransferProtocol);
        myManager = ATPManager.getInstance(myID);
        myManager.noOfPlayers = numberOfParties;
        myManager.maxBitLength = maxBitLength;
        myManager.orderNetwork = myNetworkManager.createExtraNetwork("ATPManager");
        myManager.dealNetwork = myNetworkManager.createExtraNetwork("DealNetwork");
        myManager.createAmountMap(amount);
        return this;
    }


    @Override
    public MPCBuilder<ObjectT> withNetwork(Map<Integer, Party> parties, Party myParty) throws ParseException{
        super.withNetwork(parties, myParty);
        return this;
    }

    /**
     * This function has to be implemented in each child
     * @return The Object of the class which is built here
     */
    public abstract ObjectT build();

}
