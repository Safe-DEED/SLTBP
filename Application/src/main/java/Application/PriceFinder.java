package Application;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.*;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static utils.NetworkManager.getPartyMap;

/**
 * The Price Finder represents the basis for the SLTBP MPC computation. Both the host and the client application start in main.
 * The specific pricing function "singleDateFinder" is given to the protocol as a parameter and can be modified in this
 * class.
 */
public class PriceFinder {

    private static final Logger logger = LoggerFactory.getLogger(PriceFinder.class);
    private static boolean logging = true;

    private static void log(String text){
        if(logging){
            logger.info(text);
        }
    }


    /**
     * Parses NetworkConfig.json and loads the parties map
     * @param parties Map containing FRESCO party objects associated with the id as key.
     * @param networkConfig The json parsed network config file
     * @return party object associated to self
     * @throws UnknownHostException if internet check fails
     * @throws ParseException if contents of file, do not match definition
     */
    private static Party parseNetObject(Map<Integer, Party> parties, JSONArray networkConfig) throws UnknownHostException, ParseException {
        Party party;
        Party myParty = null;
        for(Object obj : networkConfig){
            int id = Integer.parseInt((String) ((JSONObject)obj).get("id"));
            int port = Integer.parseInt((String) ((JSONObject)obj).get("port"));
            String ip = (String) ((JSONObject)obj).get("ip");
            InetAddress.getByName(ip);
            party = new Party(id, ip, port);
            if(parties.containsKey(id)){
                throw new ParseException("Party ids must be unique");
            } if (((JSONObject)obj).get("myID") instanceof Boolean && (Boolean) ((JSONObject)obj).get("myID")){
                if(myParty != null){
                    throw new ParseException("Two parties are selected!");
                }
                myParty = party;
            }
            parties.put(id, party);
        }
        if(myParty == null){
            throw new ParseException("No Party is selected: add myID = true");
        }
        return myParty;
    }

    /**
     * Checks schema of MPCSettings JSON
     * @param settings The json parsed Settings file
     * @throws ParseException if schema of MPCSettings json is wrong
     */
    private static void checkSchema(JSONObject settings) throws ParseException{
        boolean check;
        check = settings.containsKey("evaluationProtocol");
        check &= settings.containsKey("preprocessing");
        check &= settings.containsKey("otProtocol");
        check &= settings.containsKey("evaluationStrategy");
        check &= settings.containsKey("maxBitLength");
        check &= settings.containsKey("modBitLength");
        check &= settings.containsKey("benchmarking");
        check &= settings.containsKey("debug");
        if(!check){
            throw new ParseException("MPCSettings.json does not follow schema");
        }
    }

    /**
     * Parser for the MPCSettings json.
     * @param params Mutable BuilderParams object. Here the results are stored.
     * @param settings_ JSONObject of json parsed MPCSettings file.
     * @throws ParseException if there are wrong keywords or if the data types are wrong.
     */
    private static void parseMPCSettings(CmdLineParser.BuilderParams params, JSONObject settings_) throws ParseException{

        JSONObject settings = (JSONObject) settings_.get("settings");
        String evaluationProtocol, preprocessing, otProtocol, evaluationStrategy;
        int maxBitLength, modBitLength;
        boolean benchmarking, debug;
        boolean check;
        checkSchema(settings);

        log("schema check successful");
        Object evaluationProtocolObject = settings.get("evaluationProtocol");
        check = evaluationProtocolObject instanceof String;
        if(check){
            evaluationProtocol = (String) evaluationProtocolObject;
            EvaluationProtocol evaluationProtocol1 = Enum.valueOf(EvaluationProtocol.class, evaluationProtocol);
            params.setEvaluationProtocol(evaluationProtocol1);
            log("entering evalProtocol: " + evaluationProtocol1);
        }

        Object preprocessingObject = settings.get("preprocessing");
        check &= preprocessingObject instanceof String;
        if(check){
            preprocessing = (String) preprocessingObject;
            PreprocessingStrategy strategy = Enum.valueOf(PreprocessingStrategy.class, preprocessing);
            params.setPreprocessingStrategy(strategy);
            log("entering preprocessing: " + preprocessing);
        }

        Object otProtocolObject = settings.get("otProtocol");
        check &= otProtocolObject instanceof String;
        if(check){
            otProtocol = (String) otProtocolObject;
            CmdLineParser.obliviousTransferProtocol protocol = Enum.valueOf(CmdLineParser.obliviousTransferProtocol.class, otProtocol);
            params.setOtProtocol(protocol);
            log("entering otProtocol: " + otProtocol);
        }

        Object evaluationStrategyObject = settings.get("evaluationStrategy");
        check &= evaluationStrategyObject instanceof String;
        if(check){
            evaluationStrategy = (String) evaluationStrategyObject;
            EvaluationStrategy strategy = Enum.valueOf(EvaluationStrategy.class, evaluationStrategy);
            params.setEvaluationStrategy(strategy);
            log("entering evaluationStrategy: " + evaluationStrategy);
        }

        Object maxBitLengthObject = settings.get("maxBitLength");
        check &= maxBitLengthObject instanceof String;
        if(check){
            maxBitLength = Integer.parseInt((String) maxBitLengthObject);
            params.setMaxBitLength(maxBitLength);
            log("entering maxBitLength: " + maxBitLength);
        }

        Object modBitLengthObject = settings.get("modBitLength");
        check &= modBitLengthObject instanceof String;
        if(check){
            modBitLength = Integer.parseInt((String) modBitLengthObject);
            params.setModBitLength(modBitLength);
            log("entering modBitLength: " + modBitLength);
        }

        Object benchmarkingObject = settings.get("benchmarking");
        check &= benchmarkingObject instanceof Boolean;
        if(check){
            benchmarking = (Boolean) benchmarkingObject;
            params.setBenchmark(benchmarking);
            log("entering benchmarking: " + benchmarking);
        }

        Object debugObject = settings.get("debug");
        check &= debugObject instanceof Boolean;
        if(check){
            debug = (Boolean) debugObject;
            params.setDebug(debug);
            log("entering debug: " + debug);
        } else {
            throw new ParseException("Wrong types in MPCSettings.json");
        }
    }

    /**
     * Parses input from the networking options, atpunits and mpcSettings files
     * @param netPath path to the networking options file
     * @param unitsPath path to the atpUnits file
     * @param settingsPath path to the mpc settings configuration file
     * @return BuilderParams object used to instantiate the MPC instance
     */
    public static CmdLineParser.BuilderParams getParamsFromSettings(String netPath, String unitsPath, String settingsPath){
        CmdLineParser.BuilderParams params = new CmdLineParser.BuilderParams(true, false);
        JSONParser jsonParser = new JSONParser();
        JSONArray networkConfig, unitList;
        JSONObject settings;
        Party myParty;
        Map<Integer, Party> parties = new HashMap<>();

        try(FileReader reader = new FileReader(netPath)){
            networkConfig = (JSONArray) jsonParser.parse(reader);
        } catch (org.json.simple.parser.ParseException | IOException e){
            e.printStackTrace();
            return null;
        }
        try {
            myParty = parseNetObject(parties, networkConfig);
        } catch (UnknownHostException | ParseException e){
            e.printStackTrace();
            return null;
        }
        params.setHost(myParty.getPartyId() == 1);
        List<Map<Integer, Party>> list = new ArrayList<>();
        list.add(parties);
        params.setParties(list, myParty);
        params.setId(myParty.getPartyId());


        try (FileReader reader = new FileReader(unitsPath)) {
            jsonParser.reset();
            unitList =  (JSONArray) jsonParser.parse(reader);
        } catch ( org.json.simple.parser.ParseException | IOException e){
            e.printStackTrace();
            return null;
        }
        params.setUnits(unitList);

        try(FileReader reader = new FileReader(settingsPath)){
            jsonParser.reset();
            settings = (JSONObject) jsonParser.parse(reader);
        } catch ( org.json.simple.parser.ParseException | IOException e){
            e.printStackTrace();
            return null;
        }
        try {
            parseMPCSettings(params, settings);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }

        return params;    }


    /**
     * Convenience call to get Params from Settings. Calls with predefined names: "NetworkConfig.json", "ATPUnits.json", "MPCSettings.json"
     * @return Returns result from getParamsFromSettings("NetworkConfig.json", "ATPUnits.json", "MPCSettings.json") call
     */
    public static CmdLineParser.BuilderParams getParamsFromSettings(){
        return getParamsFromSettings("NetworkConfig.json", "ATPUnits.json", "MPCSettings.json");
    }


    /**
     * Entry point for the pricing protocol. Fresco params are loaded, essential instances are created and the protocol is started.
     * @throws ParseException is thrown if one of the three input files does not match schema or contains illegal entries.
     */
    public static void secureLeadTimeBasedPriceFinder() throws ParseException {
        logger.info("---------- Reading params ----------");
        CmdLineParser.BuilderParams params = getParamsFromSettings();
        logging = params.logging;
        if(params.logging){
            logger.info(params.toString());
        }
        logger.info("---------- starting setup ----------");
        BenchmarkHandler handler = BenchmarkHandler.getInstance();
        handler.startTimer(1);
        handler.startNetwork(1, 0);
        SecretDateHost secretDateHost = new DateHostBuilder(params.logging)
                .withProtocol(params.evaluationProtocol)
                .withNetwork(getPartyMap(params.partyList, params.myParty), params.myParty)
                .withResourcePool(params.preprocessingStrategy, params.modBitLength, params.otProtocol)
                .withUnits(params.units)
                .withBatchEvalStrat(params.evaluationStrategy)
                .withDebug(params.debug)
                .withBenchmark(params.benchmark)
                .withSpdzLength(params.maxBitLength)
                .build();

        logger.info("---------- Starting the protocol ----------");
        secretDateHost.runProtocol();
        handler.endTimer(1);
        handler.endNetwork(1, secretDateHost.myNetworkManager.getReceivedBytes());
    }

    /**
     * Entry point of the generalized protocol. Being host or client is defined in the Network Configuration file
     * @param args command line arguments -> none necessary
     * @throws ParseException The CustomerBuilder and HostBuilder classes both parse our input. They throw an exception
     * if they receive unexpected parameters.
     */
    public static void main(String[] args) throws ParseException {

        secureLeadTimeBasedPriceFinder();
    }

}
