package Host;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import org.apache.commons.cli.ParseException;
import utils.ATPManager;
import utils.CmdLineParser;
import utils.MPCBuilder;

import java.util.Map;


/**
 * Extension of the IFXBuilder, used to create the application of Infineon
 */
public class MPCHostBuilder extends MPCBuilder<MPCHost> {

    public MPCHostBuilder(boolean logging) {
        super(logging);
    }

    @Override
    public MPCHostBuilder withVolume(int volume, int amount){
        super.withVolume(volume, amount);
        return this;
    }

    public MPCHostBuilder withResourcePool(PreprocessingStrategy strategy, int modBitLength, ATPManager.Function priceEvaluation, CmdLineParser.obliviousTransferProtocol obliviousTransferProtocol) throws ParseException {
        super.withResourcePool(strategy, modBitLength, obliviousTransferProtocol);
        myManager.setPriceCalculation(priceEvaluation);
        return this;
    }

    @Override
    public MPCHostBuilder withNetwork(Map<Integer, Party> parties, Party myParty) throws ParseException{
        super.withNetwork(parties, myParty);
        return this;
    }


    /**
     * The build method of the builder. used to instantiate the ifxhost object according to the members in the builder.
     * @return the ifxhost object which should be fully initialized.
     */
    @Override
    public MPCHost build() {
        BatchedProtocolEvaluator<SpdzResourcePool> evaluator = new BatchedProtocolEvaluator<>(batchEvalStrat, mySuite, 4096);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce = new SecureComputationEngineImpl<>(mySuite, evaluator);
        MPCHost ifxhost          = new MPCHost();
        ifxhost.myID             = myID;
        ifxhost.maxBitLength     = maxBitLength;
        ifxhost.amount           = amount;
        ifxhost.numParties       = numberOfParties;
        ifxhost.maxVolume        = myVolume;
        ifxhost.minPrice         = myPrice;
        ifxhost.myNetwork        = myNetwork;
        ifxhost.mySce            = mySce;
        ifxhost.myDate           = myDate;
        ifxhost.myPool           = myPool;
        ifxhost.logging          = logging;
        ifxhost.myNetworkManager = myNetworkManager;
        ifxhost.myManager        = myManager;
        ifxhost.units            = units;
        return ifxhost;
    }
}
