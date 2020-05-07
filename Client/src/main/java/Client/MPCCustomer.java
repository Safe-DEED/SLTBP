package Client;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.*;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.logging.NetworkLoggingDecorator;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ATPManager;
import utils.CmdLineParser;
import utils.NetworkManager;


import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.Collections;

import static utils.NetworkManager.getPartyMap;

/**
 * IFXMPC - serving as the client side implementation of the application interface.
 * The build computation function adds the MPC functionality to the protocol builder
 */
public class MPCCustomer implements Application<Boolean, ProtocolBuilderNumeric>{

    static Logger log = LoggerFactory.getLogger(MPCCustomer.class);
    int maxBitLength;
    int myVolume;
    int myDate;
    int myP;    // price times volume for clients
    int numParties;
    int myID;
    int amount;
    ATPManager myManager = null;
    boolean logging;
    SpdzResourcePool myPool;
    Network myNetwork;
    NetworkManager myNetworkManager;
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce;

    public NetworkManager getMyNetworkManager() {
        return myNetworkManager;
    }

    public boolean isLogging() {
        return logging;
    }

    public Network getMyNetwork() {
        return myNetwork;
    }

    public SpdzResourcePool getMyPool() {
        return myPool;
    }

    public SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> getMySce() {
        return mySce;
    }


    MPCCustomer(){}

    /**
     * Distribute secret shares of my price and volume to the other parties of the network.
     * For infineon, the number of shares expected is given by the amount map, created in the ATPManager
     * @param protocolBuilderNumeric the protocol builder used to share and receive the secret shares
     */
    private void initSecretSharedValues(ProtocolBuilderNumeric protocolBuilderNumeric){
        log.info("init secret sharing values.");
        for (Map.Entry<Integer, Party> entry : myNetworkManager.getParties().entrySet()) {
            int id = entry.getKey();
            if(id == 1){
                for(int j = 0; j < myManager.amountMap.get(1); j++){
                    myManager.createUnit(id, null, null, null, protocolBuilderNumeric);  // for now the only the host has multiple units
                }
                continue;
            }
            if (myID == id){
                myManager.createUnit(id, myDate, BigInteger.valueOf(myVolume), BigInteger.valueOf(myP), protocolBuilderNumeric);
            } else {  // TODO: integrate date as a pricing factor
                myManager.createUnit(id, null, null, null, protocolBuilderNumeric);
            }
        }
        Collections.sort(myManager.unitList);
        log("unit list: " + myManager.unitList);
        log("parties: " + myNetworkManager.getParties().entrySet());
    }

    /**
     * Appends Lambda functions to the protocol builder, these functions are executed, when the protocol
     * Evaluator is called. This is done implicitly when calling the runApplication function of the SCE
     * @param producer The numeric protocol builder, where the function calles are appended
     * @return returns the function, evaluating whether the deal was possible
     */
    public DRes<Boolean> buildComputation(ProtocolBuilderNumeric producer) {
        return producer.seq(seq -> {
            initSecretSharedValues(seq);
            log("sharing the values over the network");
            return () -> null; // make void
        }).seq((seq, nil) -> {
            log("beginning arithmetic operations");
            myManager.sumWithDate(seq);
            return () -> null;
        }).seq((seq, nil) -> {
            myManager.CreateDebugLists(seq, 1);
            //myManager.EvalConditions(seq);
            log("beginning comparison operations");
            return () -> null;
        }).seq((seq, nil) -> {
            //myManager.printDebug();
            //myManager.openList(seq);
            log("finished online computation.");
            return () -> myManager.isDealPossible();
        });
    }

    private void log(String string){
        if(logging){
            log.info(string);
        }
    }

    /**
     * The main starting point of the client application. Calls the Builder to create the IFXMPC Object and
     * executes the computation of that object. The Command Line arguments used for this function are parsed in the
     * CmdLineParser class
     * @param args Command Line arguments specifying the parties, the amount, price, date, preprocessing strategy and
     *             id
     * @throws ParseException If an unexpected value occurs during the parsing, this exception is thrown, note that not all
     * edge cases are covered.
     */
    public static void main(String[] args) throws ParseException {
        CmdLineParser.BuilderParams params = CmdLineParser.GetCmdLineParams(args);

        MPCCustomer MPCCustomerDemo = new MPCCustomerBuilder(params.logging)
                .withDate(params.date)
                .withPrice(params.price)
                .withVolume(params.volume, params.hostUnits)
                .withNetwork(getPartyMap(params.partyList,params.myParty), params.myParty)
                .withResourcePool(params.preprocessingStrategy, params.modBitLength, CmdLineParser.obliviousTransferProtocol.DUMMY)
                .withSpdzLength(params.maxBitLength)
                .withBatchEvalStrat(params.evaluationStrategy)
                .build();


        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> secureComputationEngine = MPCCustomerDemo.getMySce();
        SpdzResourcePool spdzResourcePool = MPCCustomerDemo.getMyPool();
        Network network = MPCCustomerDemo.getMyNetwork();
        Duration timeout = Duration.ofMinutes(50);

        log.info("IFXMPCDEMO: " + MPCCustomerDemo);


        Boolean deal = secureComputationEngine.runApplication(
                MPCCustomerDemo, spdzResourcePool, network, timeout);

        MPCCustomerDemo.log("The result is: " + deal);
        if(deal){
            log.info("The prerequisites were met, the deal can be made");
        } else{
            log.info("The prerequisites were not met, no deal is possible");
        }
        if(MPCCustomerDemo.isLogging()){
            Long total = MPCCustomerDemo.getMyNetworkManager().getLoggedValues().get(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES);
            MPCCustomerDemo.log(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES + " " + total);
        }
    }

    @Override
    public void close() {
        this.mySce.shutdownSCE();
        if(this.myNetworkManager != null){
            myNetworkManager.close();
        }
    }
}
