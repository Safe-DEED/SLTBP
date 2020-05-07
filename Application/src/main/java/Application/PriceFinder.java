package Application;

import Client.MPCCustomer;
import Client.MPCCustomerBuilder;
import Host.MPCHost;
import Host.MPCHostBuilder;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static Host.ComputationRunner.parseATPUnit;
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

    private static ATPManager.Function pricingFinder = protocolBuilderNumeric -> {
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





    public static void main(String[] args) throws ParseException  {
        CmdLineParser.BuilderParams params = CmdLineParser.GetCmdLineParams(args);
        JSONParser jsonParser = new JSONParser();
        JSONArray unitList;
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce;
        SpdzResourcePool pool;
        Network net;

        Application<Boolean, ProtocolBuilderNumeric> demo;
        if(params.host){
            try (FileReader reader = new FileReader("ATPUnits.json")) {
                //Read JSON file
                unitList =  (JSONArray) jsonParser.parse(reader);
            } catch ( org.json.simple.parser.ParseException | IOException e){
                e.printStackTrace();
                return;
            }
            MPCHost hdemo = new MPCHostBuilder(params.logging)
                    .withVolume(params.volume, unitList.size())
                    .withNetwork(getPartyMap(params.partyList,params.myParty), params.myParty)
                    .withResourcePool(params.preprocessingStrategy, params.modBitLength, pricingFinder, params.otProtocol)
                    .withDate(params.date)
                    .withPrice(params.price)
                    .withBatchEvalStrat(params.evaluationStrategy)
                    .withSpdzLength(params.maxBitLength)
                    .build();
            List<ATPManager.ATPUnit> atpUnits = new ArrayList<>();
            for(Object unit : unitList){
                ATPManager.ATPUnit cunit = parseATPUnit((JSONObject) unit, hdemo.myManager);
                atpUnits.add(cunit);
            }
            hdemo.hostUnits = atpUnits;
            sce = hdemo.mySce;
            pool = hdemo.myPool;
            net = hdemo.myNetwork;
            demo = hdemo;
        } else{
            MPCCustomer idemo = new MPCCustomerBuilder(params.logging)
                    .withDate(params.date)
                    .withPrice(params.price)
                    .withVolume(params.volume, params.hostUnits)
                    .withNetwork(getPartyMap(params.partyList,params.myParty), params.myParty)
                    .withResourcePool(params.preprocessingStrategy, params.modBitLength, params.otProtocol)
                    .withSpdzLength(params.maxBitLength)
                    .withBatchEvalStrat(params.evaluationStrategy)
                    .build();
            sce = idemo.getMySce();
            pool = idemo.getMyPool();
            net = idemo.getMyNetwork();
            demo = idemo;

        }
        Boolean deal = sce.runApplication(demo, pool, net, Duration.ofMinutes(20));
        log.info("the result of the deal is: " + deal);


    }

}
