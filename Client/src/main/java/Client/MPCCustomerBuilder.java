package Client;

import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ATPManager;
import utils.CmdLineParser;
import utils.MPCBuilder;

/**
 * IFXMPCBuilder, extends the generic Builder and facilitates creating an instance of the client application: IFXMPC
 */
public class MPCCustomerBuilder extends MPCBuilder<MPCCustomer> {

    private Logger log = LoggerFactory.getLogger(MPCCustomerBuilder.class);

    public MPCCustomerBuilder(boolean logging){
        super(logging);
        //        private Supplier<Network> myNetworkSupplier;
    }

    public MPCCustomerBuilder withResourcePool(PreprocessingStrategy strategy, int modBitLength, CmdLineParser.obliviousTransferProtocol obliviousTransferProtocol) throws ParseException {
        super.withResourcePool(strategy, modBitLength, obliviousTransferProtocol);
        myManager = ATPManager.getInstance(myID);
        myManager.noOfPlayers = numberOfParties;
        myManager.maxBitLength = maxBitLength;
        myManager.orderNetwork = myNetworkManager.createExtraNetwork();
        myManager.createAmountMap(amount);
        return this;
    }

    /**
     * The ifxmpc object is created and its members are set according to the members set in the parent builder.
     * @return the fully initiated ifxmpc object.
     */
    public MPCCustomer build(){
        BatchedProtocolEvaluator<SpdzResourcePool> evaluator = new BatchedProtocolEvaluator<>(batchEvalStrat, mySuite, 4096);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce = new SecureComputationEngineImpl<>(mySuite, evaluator);
        MPCCustomer MPCCustomer = new MPCCustomer();
        MPCCustomer.myID             = myID;
        MPCCustomer.maxBitLength     = maxBitLength;
        MPCCustomer.numParties       = numberOfParties;
        MPCCustomer.amount           = amount;
        MPCCustomer.myVolume         = myVolume;
        MPCCustomer.myP              = myPrice * super.myVolume;
        MPCCustomer.myNetwork        = myNetwork;
        MPCCustomer.mySce            = mySce;
        MPCCustomer.myDate           = myDate;
        MPCCustomer.myPool           = myPool;
        MPCCustomer.myManager        = myManager;
        MPCCustomer.logging          = logging;
        MPCCustomer.myNetworkManager = myNetworkManager;
        log.info("SETUP for client finished!");
        return MPCCustomer;
    }
}