package Host;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ATPManager;
import utils.CmdLineParser;
import utils.NetworkManager;

import java.util.*;

/**
 * This is the Infineon counter part to the IFXMPC class. NOTE: could be simplified using inheritance
 * maybe one ifx super class...
 *
 * implements the Application interface, to build the MPC computation.
 */
public class MPCHost implements Application<Integer, ProtocolBuilderNumeric> {

    private static Logger log = LoggerFactory.getLogger(MPCHost.class);
    int maxVolume;
    int minPrice;
    int maxBitLength;
    int myDate;
    int numParties;
    int myID;
    int amount;
    public List<ATPManager.ATPUnit> units;
    boolean logging;
    public ATPManager myManager;
    public SpdzResourcePool myPool;
    public Network myNetwork;
    public NetworkManager myNetworkManager;
    public SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce;



    /**
     * Build each step of the computation, each step will be executed asynchronously. Inputs outputs and
     * Comparison results can only be used in the next step, since only then the completion of their evaluation is guaranteed
     * @param producer The numeric protocol builder used to create the MPC protocol
     * @return returns the Lambda function evaluating whether the deal could be made
     */
    public DRes<Integer> buildComputation(ProtocolBuilderNumeric producer) {
        return producer.seq(seq -> {
            log("sharing values over the network");
            for (Map.Entry<Integer, Party> entry : myNetworkManager.getParties().entrySet()) {
                int id = entry.getKey();
                if(id == 1) {
                    for(int j = 0; j < amount; j++){
                        myManager.createUnit(units.get(j), seq);  // for now the host only has one stock!
                    }
                    continue;
                }
                for (int j = 0; j < myManager.amountMap.get(id); j++){
                    myManager.createUnit(id, null, null, null,seq);
                }
           }
            Collections.sort(myManager.unitList);
            log(myManager.toString());
            return () -> null;
        }).seq((seq, list) -> {
            //myManager.sumWithDate(seq);
            log("sum Individual date");
            myManager.sumIndividualDate(seq);
            return () -> null;
        }).seq((seq, list) -> {
            //myManager.CreateDebugLists(seq, 1);
            //myManager.EvalConditions(seq);
            log("openPrice and Vol");
            myManager.openPriceAndVolSum(seq);
            return () -> null;
        }).seq((seq, list) -> {
            log("evaluate protocol");
            //myManager.printDebug();
            //myManager.openList(seq);
            ATPManager.OpenStatus status = myManager.OpenEvaluation(seq);
            return () -> myManager.isDealPossible(status);
        }).seq((seq, in) -> {
            log("The result was " + in);
            return () -> in;
        });
    }

    private void log(String string){
        if(logging){
            log.info(string);
        }
    }

    public static void RunComputation(ATPManager.Function pricingFunction, String[] args) throws ParseException {
        CmdLineParser.BuilderParams params = CmdLineParser.GetCmdLineParams(args);
        params.setPricingFunction(pricingFunction);

        //List<Map<Integer, Party>> partyList = createPartyMap(params.parties, params.id);
        if(!params.multiThreaded){
            new ComputationRunner(params,"host", 0, log).start();
        } else {
            for(int i = 0; i < params.partyList.size(); i++){
                ComputationRunner runner = new ComputationRunner(params, params.partyList.get(i).keySet().toString(), i, log);
                runner.start();
            }
        }

    }


    /**
     * The main entry point for infieon. The params are parsed from the cmd line but could also be constructed in a different
     * manner. The application is run in the computation runner, enabling for multithreaded execution (only faster, if the set of
     * ATPUnits can be separated)
     * @param args The command line arguments used to initiate the IFXHost object
     * @throws ParseException Throws a parse exception if an unexpected value occured or if something necessary is missing
     */
    public static void main(String[] args) throws ParseException {
        RunComputation(protocolBuilderNumeric -> {
            log.info("Lambda function -> IFXHOST main called");
        },args);
    }


    @Override
    public void close() {
        this.mySce.shutdownSCE();
        if(this.myNetworkManager != null){
            myNetworkManager.close();
        }
    }
}
