package utils;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import org.apache.commons.cli.*;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Utility class to gather all the builder parameters necessary for the applications from the command line.
 */
public class CmdLineParser {
    private static final String ID             = "i";
    private static final String PARTY          = "p";
    private static final String PRICE          = "P";
    private static final String VOLUME         = "V";
    private static final String DAY            = "T";
    private static final String HOSTAMOUNT     = "A";
    private static final String HOSTID         = "H";
    private static final String LOGGING        = "l";
    private static final String MULTITHREADED  = "m";
    private static final String PROPERTIES     = "D";
    private static final String IDMSG          = "The id of this player. Must be a unique positive integer.";
    private static final String PARTYMSG       = "Connection data for a party. Use -p multiple times to specify many players. You must always at least include yourself. Must be on the form [id]:[hostname]:[port]. id is a unique positive integer for the player, host and port is where to find the player";
    private static final String PRESTRATMSG    = "Used to set the preprocessing Strategy of SPDZ";
    private static final String LOGGINGMSG     = "Informs FRESCO that performance logging should be triggered";
    private static final String PRICEMSG       = "The Price a party is willing to pay, or the minimum Price if id = 1";
    private static final String VOLUMEMSG      = "The Volume a party is willing to order, or the maximum available volume";
    private static final String DAYMSG         = "The date of order";
    private static final String HOSTMSG        = "The hosting party of this deal";
    private static final String MULTIMSG       = "Determines wether the pool is separated into multiple sub-pools";
    private static final String AMOUNTMSG      = "Determines the amount of ATP Units the host provides.";
    private static final String IDERRMSG       = "ID must be positive";
    private static final String PARTYERRMSG    = "Party ids must be unique";
    private static final String MODBITERRMSG   = "Spdz.modBitLength must be > 32";
    private static final String MAXBITERRMSG   = "spdz.maxBitLength must be > 1";
    private static final String PRICEERRMSG    = "Price must be a positive Integer";
    private static final String VOLUMEERRMSG   = "Volume must be a positive Integer";
    private static final String DAYERRMSG      = "The date should specify the days until delivery: For now must be between 1 and 10";
    private static final String HOSTERRMSG     = "The amount of units for the host has to be between 1 and 10";
    private static Logger log = LoggerFactory.getLogger(CmdLineParser.class);
    private static int newID = 0;
    public enum obliviousTransferProtocol {NAOR, DUMMY}

    /**
     * Checks on a basic level if the party input after -p is in the correct form
     * @param partyOption The option provided after this -p argument
     * @return Returns the party parsed from the partyOption
     * @throws ParseException If the partyOption is not compliant with the expected format
     */
    private static Party checkParty(String partyOption) throws ParseException {
        String[] p = partyOption.split(":");
        if(p.length != 3){
            throw new ParseException("Could not parse '" + partyOption + "' as [id]:[host]:[port]");
        }
        try {
            int id = Integer.parseInt(p[0]);
            InetAddress.getByName(p[1]); // Check that hostname is valid.
            return new Party(id, p[1], Integer.parseInt(p[2]));
        } catch (NumberFormatException | UnknownHostException e) {
            throw new ParseException("Could not parse '" + partyOption + "': " + e.getMessage());
        }
    }


    /**
     * Main functionality to read the input arguments and parse the Builder Parameters. Every option is required but the multithreaded
     * and logging flag. the correct format of each option can be read below or int the examples given in the makefile
     * @param args the command line arguments provided when starting up the program
     * @return Returns a BuilderParams object, providing everything the builders need.
     * @throws ParseException throws this exception if some value is not as expected
     */
    public static BuilderParams GetCmdLineParams(String[] args) throws ParseException {
        //Define possible CommandLine options
        Options options = new Options();
        options.addOption(Option.builder(ID).desc(IDMSG).longOpt("id").required(true).hasArg().build());
        options.addOption(Option.builder(PARTY).desc(PARTYMSG).longOpt("party").required(true).hasArgs().build());
        options.addOption(Option.builder(PROPERTIES).desc(PRESTRATMSG).required(false).hasArg().numberOfArgs(2).valueSeparator().build());
        options.addOption(Option.builder(LOGGING).desc(LOGGINGMSG).required(false).hasArg(false).build());
        options.addOption(Option.builder(PRICE).desc(PRICEMSG) .required(false).longOpt("price").hasArg().build());
        options.addOption(Option.builder(VOLUME).desc(VOLUMEMSG).required(false).longOpt("volume").hasArg().build());
        options.addOption(Option.builder(DAY).desc(DAYMSG)   .required(false).longOpt("date").hasArg().build());
        options.addOption(Option.builder(HOSTID).desc(HOSTMSG)  .required(false).longOpt("host").hasArg(false).build());
        options.addOption(Option.builder(MULTITHREADED).desc(MULTIMSG) .required(false).hasArg(false).longOpt("multithreaded").build());
        options.addOption(Option.builder(HOSTAMOUNT).desc(AMOUNTMSG).required(false).hasArg(true).build());

        // Parse Command line input into CommandLine format
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Extract input values from cmd
        int myid, price, volume, date, hostAmount, maxBitLength, modBitLength;
        boolean logging = cmd.hasOption(LOGGING);
        boolean multiThreaded = cmd.hasOption(MULTITHREADED);
        boolean host = cmd.hasOption(HOSTID);
        final Map<Integer, Party> parties = new HashMap<>();
        List<Map<Integer, Party>> partyList = new ArrayList<>();
        Party myParty;
        PreprocessingStrategy strategy;
        obliviousTransferProtocol otProtocol;

        // Build the IFX.MPC.IFX.Client.IFXMPC using the parsed values
        BuilderParams params = new BuilderParams(logging, multiThreaded, host);

        myid = Integer.parseInt(cmd.getOptionValue(ID));
        if (myid < 1) {throw new ParseException(IDERRMSG);}
        params.setId(myid);

        if(cmd.hasOption(PRICE)){
            price = Integer.parseInt(cmd.getOptionValue(PRICE));
            if (price < 1) {throw new ParseException(PRICEERRMSG);}
            params.setPrice(price);
        }
        if(cmd.hasOption(VOLUME)){
            volume = Integer.parseInt(cmd.getOptionValue(VOLUME));
            if (volume < 1) {throw new ParseException(VOLUMEERRMSG);}
            params.setVolume(volume);
        }
        if(cmd.hasOption(DAY)){
            date = Integer.parseInt(cmd.getOptionValue(DAY));
            if (date < 1 || date > 10) {throw new ParseException(DAYERRMSG);}
            params.setDate(date);
        }
        if(cmd.hasOption(HOSTAMOUNT)){
            hostAmount = Integer.parseInt(cmd.getOptionValue(HOSTAMOUNT));
            if(hostAmount < 1 || hostAmount > 10) {throw new ParseException(HOSTERRMSG);}
            params.setHostUnits(hostAmount);

        }

        Properties spdzProperties;
        if(cmd.hasOption(PROPERTIES)){
            spdzProperties = cmd.getOptionProperties(PROPERTIES);
            strategy = PreprocessingStrategy.valueOf(spdzProperties.getProperty("spdz.preprocessingStrategy", PreprocessingStrategy.DUMMY.toString()));
            otProtocol = obliviousTransferProtocol.valueOf(spdzProperties.getProperty("obliviousTransfer", obliviousTransferProtocol.DUMMY.toString()));
            maxBitLength = Integer.parseInt(spdzProperties.getProperty("spdz.maxBitLength", "64"));
            modBitLength = Integer.parseInt(spdzProperties.getProperty("spdz.modBitLength", "64"));
            if (maxBitLength < 2) {throw new ParseException(MAXBITERRMSG);}
            if (modBitLength < 33) {throw new ParseException(MODBITERRMSG);}
            params.setMaxBitLength(maxBitLength);
            params.setModBitLength(modBitLength);
            params.setPreprocessingStrategy(strategy);
            params.setOtProtocol(otProtocol);
        }

        // check if all parties are entered correctly
        for (String partyOption : cmd.getOptionValues(PARTY)) {
            Party party = checkParty(partyOption);
            if(parties.containsKey(party.getPartyId())){
                throw new ParseException(PARTYERRMSG);
            }
            parties.put(party.getPartyId(), party);
        }
        myParty = parties.get(myid);
        if(myParty == null){
            throw new ParseException("This party is given the id " + myid
                    + " but this id is not present in the list of parties: ");
        }
        if(multiThreaded){
            partyList = createPartyMap(parties, myid);
            myParty = new Party(newID, myParty.getHostname(), myParty.getPort());
        } else {
            partyList.add(parties);
        }

        params.setParties(partyList, myParty);
        params.setEvaluationStrategy(EvaluationStrategy.SEQUENTIAL);
        return params;
    }


    /**
     * In case the application is executed using multi threading, this function separates the parties into different party maps.
     * in these different maps, the ids are newly set, since in the framework the ids have to contain 1 and have to ascend from one on.
     * If this is to be changed, one has to replace the socket network of the framework with an independent implementation.
     * @param parties the current party map which is to be separated
     * @param myID the id this user currently possesses
     * @return a list of new party maps, one for each subgroup of clients
     *
     * NOTE: This function has to be exchanged/modified if one wants to change the logic of separation (e.g. separation by date)
     * Now separation is by id: (1,2,3), (1,4,5) ... 1 is infineon and has to be present in every map
     */
    private static List<Map<Integer, Party>> createPartyMap(Map<Integer, Party> parties, int myID){
        List<Map<Integer, Party>> partiesList = new ArrayList<>();
        Map<Integer, Party> parties_ = new HashMap<>();
        int hostID = 1;
        int cnt = parties.size();
        int id_cnt = 2;
        for(Map.Entry<Integer, Party> entry : parties.entrySet()){
            cnt--;
            if(entry.getKey() != hostID) {
                parties_.put(id_cnt, new Party(id_cnt, entry.getValue().getHostname(), entry.getValue().getPort()));
                if(entry.getKey() == myID){
                    newID = id_cnt;
                }
                id_cnt++;
            } if (parties_.size() == 2 && cnt > hostID){
                parties_.put(hostID, parties.get(hostID));
                partiesList.add(parties_);
                //log.info("map: " + parties_.keySet());
                //log.info("map: " + parties_.values());
                id_cnt = 2;
                parties_ = new HashMap<>();
            }
        }
        parties_.put(hostID, parties.get(hostID));
        //log.info("map: " + parties_.keySet());
        //log.info("map: " + parties_.values());
        partiesList.add(parties_);

        return partiesList;
    }


    /**
     * A class providing storage for all the parameters the applications need to be properly initialized.
     * This includes parameters used in the framework as well as parameters which are use case specific.
     *
     */
    public static class BuilderParams{
        public boolean logging;
        public int id;
        public int price;
        public int volume;
        public int amount;
        public int date;
        public int hostUnits;
        public Party myParty;
        public Map<Integer, Party> parties;
        public List<Map<Integer, Party>> partyList;
        public JSONArray units;
        public int maxBitLength;
        public int modBitLength;
        public boolean host;
        public PreprocessingStrategy preprocessingStrategy;
        public obliviousTransferProtocol otProtocol;
        public EvaluationStrategy evaluationStrategy;
        public ATPManager.Function pricingFunction;
        public boolean multiThreaded;

        public BuilderParams(boolean logging, boolean multiThreaded, boolean host){
            this(logging, multiThreaded);
            setHost(host);
        }
        public BuilderParams(boolean logging, boolean multiThreaded){
            this.logging = logging;
            this.multiThreaded = multiThreaded;
        }
        public void setId(int id){ this.id = id; }
        public void setHostUnits(int units) {this.hostUnits = units;}
        public void setPrice(int price){ this.price = price; }
        public void setVolume(int volume){ this.volume = volume; }
        public void setUnits(JSONArray units){
            this.units = units;
            this.amount = units.size();
        }
        public void setDate(int date){ this.date = date; }
        public void setParties(List<Map<Integer, Party>> partyList, Party party){
            this.myParty = party;
            this.partyList = partyList;
            if(!multiThreaded){
                this.parties = partyList.get(0);
            }
        }
        public void setHost(Boolean host){
            this.host = host;
        }
        public void setOtProtocol(obliviousTransferProtocol otProtocol){
            this.otProtocol = otProtocol;
        }
        public void setPricingFunction(ATPManager.Function pricingFunction){this.pricingFunction = pricingFunction;}
        public void setMaxBitLength(int maxBitLength){ this.maxBitLength = maxBitLength; }
        public void setModBitLength(int modBitLength){ this.modBitLength = modBitLength; }
        public void setPreprocessingStrategy(PreprocessingStrategy strategy){ this.preprocessingStrategy = strategy; }
        public void setEvaluationStrategy(EvaluationStrategy strategy){ this.evaluationStrategy = strategy; }
        public String toString(){
            return "logging: " + logging +
                    "\nhost " + host +
                    "\nid " + id +
                    "\nprice " + price +
                    "\nvolume " + volume +
                    "\namount " + amount +
                    "\ndate " + date +
                    "\nhostUnits " + hostUnits +
                    "\nmyParty " + myParty +
                    "\nparties " + parties +
                    "\npartyList " + partyList +
                    "\nmaxBitLength " + maxBitLength +
                    "\nmodBitLength " + modBitLength +
                    "\npreprocessing " + preprocessingStrategy +
                    "\notProtocol " + otProtocol +
                    "\nevaluation " + evaluationStrategy +
                    "\npricingFunction " + pricingFunction +
                    "\nmultithreading " + multiThreaded;
        }
    }

}
