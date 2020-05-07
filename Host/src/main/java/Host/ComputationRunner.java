package Host;

import dk.alexandra.fresco.logging.NetworkLoggingDecorator;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import utils.ATPManager;
import utils.CmdLineParser;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A thread implementation enabling the infineon application to be run in parallel. Even if there is only one instance,
 * the computation is run by this implementation.
 */
public class ComputationRunner implements Runnable{
    private Thread t;
    private CmdLineParser.BuilderParams params;
    private String threadName;
    private int partyIndex;
    private Logger log;

    /**
     * The constructor, initiating the computation runner, this is necessary to have the cmdLine args and name of the thread
     * @param params Parameters used to build the IFXHost object. Can be loaded via cmdLine, but could also be read from other sources.
     * @param name A name used in log msgs of this thread.
     * @param listIndex When using the current multithreaded implementation, the listindex determines the map of parties for this thread
     *                  in the given partyMapList which is in the params.
     * @param log The logger of the main host function, which is used to log the info to the same location
     */
    public ComputationRunner(CmdLineParser.BuilderParams params, String name, int listIndex, Logger log){
        this.params = params;
        this.threadName = name;
        this.partyIndex = listIndex;
        this.log = log;
    }

    /**
     * the threads run method. Here the Host Object is truly built using the params. Then the supplied units of infineon are
     * parsed from JSON and the Application is run.
     */
    public void run() {
        log.info("Running computation of: " + params.partyList.get(partyIndex).keySet());
        MPCHost MPCHostDemo;
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("ATPUnits.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONArray unitList = (JSONArray) obj;
            log.info(unitList.toString());


            MPCHostDemo = new MPCHostBuilder(params.logging)
                    .withVolume(params.volume, unitList.size())
                    .withNetwork(params.partyList.get(partyIndex), params.partyList.get(partyIndex).get(params.id))
                    .withResourcePool(params.preprocessingStrategy, params.modBitLength, protocolBuilderNumeric -> {
                        System.out.println("The anonymous function was called");
                    }, params.otProtocol)
                    .withDate(params.date)
                    .withPrice(params.price)
                    .withBatchEvalStrat(params.evaluationStrategy)
                    .withSpdzLength(params.maxBitLength)
                    .build();

            //Iterate over employee array
            List<ATPManager.ATPUnit> atpUnits = new ArrayList<>();
            for(Object unit : unitList){
                ATPManager.ATPUnit cunit = parseATPUnit((JSONObject) unit, MPCHostDemo.myManager);
                atpUnits.add(cunit);
                log.info(cunit.toString());
            }
            MPCHostDemo.hostUnits = atpUnits;

        } catch (IOException | org.json.simple.parser.ParseException | ParseException e) {
            //e.printStackTrace(); // testing stack trace

            return;
        }
        




       Boolean deal = MPCHostDemo.mySce.runApplication(MPCHostDemo, MPCHostDemo.myPool, MPCHostDemo.myNetwork, Duration.ofMinutes(50));

        if(deal){
            log.info(threadName + " :the following parts have to be delivered: " + MPCHostDemo.myManager.unitList);
        } else {
            log.info(threadName + ": The deal can't be made");
        }

        if(MPCHostDemo.logging){
            Long total = MPCHostDemo.myNetworkManager.getLoggedValues().get(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES);
            log.info(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES + " " + total);
        }

    }

    /**
     * The threads start method, here the run method is called.
     */
    public void start() {
        if(t == null){
            log.info("starting thread: " + threadName);
            t = new Thread(this, threadName);
            t.start();
        }
    }

    /**
     * The ATP Units of the Host(Infineon) are parsed from a JSON file
     * @param unit the current JSON Object, which should contain a unit
     * @param manager The ATPManager instance, used to create a new ATP Unit
     * @return the newly created ATPUnit object with the parsed contents
     */
    public static ATPManager.ATPUnit parseATPUnit(JSONObject unit, ATPManager manager)
    {
        //Get employee object within list
        JSONObject atpUnit = (JSONObject) unit.get("unit");

        //Get employee first name
        int price = Integer.parseInt((String) atpUnit.get("price"));

        //Get employee last name
        int amount = Integer.parseInt((String) atpUnit.get("amount"));

        //Get employee website name
        int date = Integer.parseInt((String) atpUnit.get("date"));
        return manager.new ATPUnit(1, date, BigInteger.valueOf(amount), BigInteger.valueOf(price));
    }
}