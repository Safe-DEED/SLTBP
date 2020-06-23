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
import utils.ATPManager;
import utils.CmdLineParser;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.ATPManager.parseATPUnit;
import static utils.NetworkManager.getPartyMap;

public class PriceFinder {

    private static Logger log = LoggerFactory.getLogger(PriceFinder.class);

    private static ATPManager instance;

    private static ATPManager getATPInstance(){
        if(instance == null){
            instance = ATPManager.getInstance(1);
        }
        return instance;
    }

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
            found = i + 1;
            break;
        }
        man.selectedDeal = found;
        if(found == -1){
            throw new RuntimeException("");
        }
    };

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

    public static CmdLineParser.BuilderParams getDefaultParams(String netPath, String unitsPath){
        CmdLineParser.BuilderParams params = new CmdLineParser.BuilderParams(false, false);
        params.setMaxBitLength(10);
        params.setModBitLength(128);
        params.setPreprocessingStrategy(PreprocessingStrategy.MASCOT);
        params.setOtProtocol(CmdLineParser.obliviousTransferProtocol.NAOR);
        params.setEvaluationStrategy(EvaluationStrategy.SEQUENTIAL);
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
            params.setHost(myParty.getPartyId() == 1);
        } catch (UnknownHostException | ParseException e){
            e.printStackTrace();
            return null;
        }
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


    public static void main(String[] args) throws ParseException  {
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce;
        SpdzResourcePool pool;
        Network net;
        CmdLineParser.BuilderParams params1 = getDefaultParams();
        Application<Integer, ProtocolBuilderNumeric> demo;
        log.info("---------- starting setup ----------");
        if(params1.host){
            MPCHost hdemo = new MPCHostBuilder(params1.logging)
                    .withVolume(params1.volume, params1.amount)
                    .withNetwork(getPartyMap(params1.partyList,params1.myParty), params1.myParty)
                    .withResourcePool(params1.preprocessingStrategy, params1.modBitLength, singleDateFinder, params1.otProtocol)
                    .withDate(params1.date)
                    .withPrice(params1.price)
                    .withUnits(params1.units)
                    .withBatchEvalStrat(params1.evaluationStrategy)
                    .withSpdzLength(params1.maxBitLength)
                    .build();

            sce = hdemo.mySce;
            pool = hdemo.myPool;
            net = hdemo.myNetwork;
            demo = hdemo;
        } else{
            MPCCustomer idemo = new MPCCustomerBuilder(params1.logging)
                    .withDate(params1.date)
                    .withPrice(params1.price)
                    .withVolume(params1.volume, params1.amount)
                    .withNetwork(getPartyMap(params1.partyList,params1.myParty), params1.myParty)
                    .withResourcePool(params1.preprocessingStrategy, params1.modBitLength, params1.otProtocol)
                    .withUnits(params1.units)
                    .withSpdzLength(params1.maxBitLength)
                    .withBatchEvalStrat(params1.evaluationStrategy)
                    .build();
            sce = idemo.getMySce();
            pool = idemo.getMyPool();
            net = idemo.getMyNetwork();
            demo = idemo;
        }
        log.info("---------- Starting the protocol ----------");
        Integer deal = sce.runApplication(demo, pool, net, Duration.ofMinutes(20));
        if(deal > 0){
            log.info("the resulting deal is: " + deal);
        } else{
            log.info("No deal can be made");
        }



    }

}
