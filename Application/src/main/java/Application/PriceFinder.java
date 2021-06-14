package Application;

import Client.MPCCustomer;
import Client.MPCCustomerBuilder;
import Host.MPCHost;
import Host.MPCHostBuilder;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
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
    private static ATPManager instance;

    /**
     * As the ATP Manager represents the stateful logic of the computation it is implemented as a singleton.
     * Here we defined a wrapper storing the instance for convenience.
     * @return the ATPManager instance of this application
     */
    private static ATPManager getATPInstance(){
        if(instance == null){
            instance = ATPManager.getInstance(1);
        }
        return instance;
    }

    private static void log(String text){
        if(logging){
            logger.info(text);
        }
    }

    /**
     * singleDateFinder is an implementation of the ATPManager Function interface. Functions that implement this interface
     * must evaluate the pricing problem.
     */
    private static final ATPManager.Function singleDateFinder = protocolBuilderNumeric -> {
        ATPManager man = ATPManager.getInstance(1);
        BigInteger currentVol, currentPrice, minPrice, currentHostPrice;
        Integer found = -1;
        for (int i = 0; i < man.clientVolumeSum.size(); i++){
            currentVol = man.VolSum.get(i).out();

            if(currentVol.compareTo(man.hostVol.get(i)) > 0){
                System.out.println("volume fail at: " + i);
                continue;
            }
            currentPrice = man.PriSum.get(i).out();
            currentHostPrice = man.hostPrice.get(i);
            minPrice = currentVol.multiply(currentHostPrice);
            if(minPrice.compareTo(currentPrice) > 0){
                System.out.println("price fail at: " + i);
                continue;
            }
            found = i;
            break;
        }
        man.selectedDeal = man.dateList.get(found);
        if(found.equals(-1)){
            throw new RuntimeException("");
        }
    };

    /**
     * The pricingFinder is an alternative pricing evaluation. As this protocol is not at its final stage, this
     * implementation is kept in order to have a backup.
     */
    private static final ATPManager.Function pricingFinder = protocolBuilderNumeric -> {
        int lOS, cOS;
        BigInteger costDiff;
        for (int i = 0; i < getATPInstance().DEBUGMinCost.size(); i++) {
            getATPInstance().printDebug();
            lOS = (getATPInstance().DEBUGLeftOver.get(i).out().compareTo( BigInteger.ZERO));

            if(lOS != 1){
                System.out.println("comparison failed!");
                throw new IllegalArgumentException("not enough volume supplied!");
            }
            lOS = (getATPInstance().DEBUGLeftOver.get(i).out().compareTo( new BigInteger("1000000000000000")));
            if(lOS != -1){ // no sign conversion is done yet in the framework - this is a problem in the current version of Fresco but will be fixed in the next release
                System.out.println("comparison failed!");
                throw new IllegalArgumentException("not enough volume supplied!");
            }
            costDiff = getATPInstance().DEBUGCost.get(i).out().subtract(getATPInstance().DEBUGMinCost.get(i).out());
            cOS = costDiff.compareTo( BigInteger.ZERO);
            if(cOS == -1){
                System.out.println("comparison failed!");
                throw new IllegalArgumentException("The price suggested by the customers was too low!");
            }

        }

    };

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


    private static void parseMPCSettings(CmdLineParser.BuilderParams params, JSONObject settings_) throws ParseException, IllegalArgumentException{

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
     * FRESCO is a very context heavy framework. In the early versions of this project, a lot of settings were set as command
     * line arguments. This function provides us with the default arguments without the need of such command line heavy interactions.
     * @param netPath The path to the network configuration json file
     * @param unitsPath The path to the ATPUnits json file. The data for the protocol.
     * @return Builderparams object, containing the necessary fields to setup the framework and the protocol.
     */
    public static CmdLineParser.BuilderParams getDefaultParams(String netPath, String unitsPath){
        CmdLineParser.BuilderParams params = new CmdLineParser.BuilderParams(true, false);
        params.setMaxBitLength(64);
        params.setModBitLength(128);
        params.setPreprocessingStrategy(PreprocessingStrategy.MASCOT);
        params.setOtProtocol(CmdLineParser.obliviousTransferProtocol.NAOR);
        params.setEvaluationStrategy(EvaluationStrategy.SEQUENTIAL_BATCHED);
        params.setEvaluationProtocol(EvaluationProtocol.LINEAR);
        params.setDebug(false);
        params.setBenchmark(false);
        JSONParser jsonParser = new JSONParser();
        JSONArray networkConfig, unitList;
        Party party;
        Party myParty = null;
        Map<Integer, Party> parties = new HashMap<>();
        //List<ATPManager.ATPUnit> atpUnits = new ArrayList<>();

        try(FileReader reader = new FileReader(netPath)){
            networkConfig = (JSONArray) jsonParser.parse(reader);
        } catch (org.json.simple.parser.ParseException | IOException e){
            e.printStackTrace();
            return null;
        }
        try {
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
        } catch (UnknownHostException | ParseException e){
            e.printStackTrace();
            return null;
        }
        params.setHost(myParty.getPartyId() == 1);
        List<Map<Integer, Party>> list = new ArrayList<>();
        list.add(parties);
        params.setParties(list, myParty);
        params.setId(myParty.getPartyId());
        params.setDate(0);      //
        params.setPrice(0);     //
        params.setVolume(0);    // These functions should be removed once file based input is here
        params.setHostUnits(0); //


        try (FileReader reader = new FileReader(unitsPath)) {
            jsonParser.reset();
            unitList =  (JSONArray) jsonParser.parse(reader);
        } catch ( org.json.simple.parser.ParseException | IOException e){
            e.printStackTrace();
            return null;
        }


        params.setUnits(unitList);
        return params;
    }

    public static CmdLineParser.BuilderParams getDefaultParams(){
        return getDefaultParams("NetworkConfig.json", "ATPUnits.json");
    }

    public static CmdLineParser.BuilderParams getParamsFromSettings(){
        return getParamsFromSettings("NetworkConfig.json", "ATPUnits.json", "MPCSettings.json");
    }

    public static void volumePriceAggregator() throws ParseException{
    /**
     * Entry point of the generalized protocol. Being host or client is defined in the Network Configuration file
     * @param args command line arguments -> none necessary
     * @throws ParseException The CustomerBuilder and HostBuilder classes both parse our input. They throw an exception
     * if they receive unexpected parameters.
     */
        logger.info("---------- Reading params ----------");
        CmdLineParser.BuilderParams params = getDefaultParams();
        logging = params.logging;
        if(params.logging){
            logger.info(params.toString());
        }
        BenchmarkHandler handler = BenchmarkHandler.getInstance();
        handler.startTimer(1);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce;
        SpdzResourcePool pool;
        Network net;
        NetworkManager manager;
        Application<Integer, ProtocolBuilderNumeric> demo;
        logger.info("---------- starting setup ----------");
        handler.startNetwork(params.id, 0);
        if(params.host){
            MPCHost hdemo = new MPCHostBuilder(params.logging)
                    .withVolume(params.volume, params.amount)
                    .withNetwork(getPartyMap(params.partyList,params.myParty), params.myParty)
                    .withResourcePool(params.preprocessingStrategy, params.modBitLength, singleDateFinder, params.otProtocol)
                    .withDate(params.date)
                    .withPrice(params.price)
                    .withUnits(params.units)
                    .withBatchEvalStrat(params.evaluationStrategy)
                    .withSpdzLength(params.maxBitLength)
                    .build();

            sce = hdemo.mySce;
            pool = hdemo.myPool;
            net = hdemo.myNetwork;
            manager = hdemo.myNetworkManager;
            demo = hdemo;
        } else{
            MPCCustomer idemo = new MPCCustomerBuilder(params.logging)
                    .withDate(params.date)
                    .withPrice(params.price)
                    .withVolume(params.volume, params.amount)
                    .withNetwork(getPartyMap(params.partyList,params.myParty), params.myParty)
                    .withResourcePool(params.preprocessingStrategy, params.modBitLength, params.otProtocol)
                    .withUnits(params.units)
                    .withSpdzLength(params.maxBitLength)
                    .withBatchEvalStrat(params.evaluationStrategy)
                    .build();
            sce = idemo.getMySce();
            pool = idemo.getMyPool();
            net = idemo.getMyNetwork();
            manager = idemo.getMyNetworkManager();
            demo = idemo;
        }
        logger.info("---------- Starting the protocol ----------");
        Integer deal = sce.runApplication(demo, pool, net, Duration.ofMinutes(20));
        handler.endTimer(1);
        handler.endNetwork(params.id, manager.getReceivedBytes());
        if(deal > 0){
            logger.info("the resulting deal is: " + deal);
        } else{
            logger.info("No deal can be made");
        }
    }


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

        //volumePriceAggregator();
        secureLeadTimeBasedPriceFinder();
    }

}
