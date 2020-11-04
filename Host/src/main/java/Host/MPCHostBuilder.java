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

    /**
     * Setting the volume
     * @param volume the requested amount or the provided amount
     * @param amount the number of atp units provided - if > 1 then volume and price are ignored
     * @return HOST Builder instance
     */
    @Override
    public MPCHostBuilder withVolume(int volume, int amount){
        super.withVolume(volume, amount);
        return this;
    }

    /**
     * ResourcePool setter. Overwritten in order to add the pricing function to the ATPManager. The pricing function is
     * evaluated in the end of the protocol.
     * @param strategy Preprocessing strategy as defined by FRESCO
     * @param modBitLength Bit length of the underlying modulus
     * @param priceEvaluation Evaluation function of the protocol
     * @param obliviousTransferProtocol OT protocol used by FRESCO
     * @return  HostBuilder instance
     * @throws ParseException unknown inputs to FRESCO result in a ParseException
     */
    public MPCHostBuilder withResourcePool(PreprocessingStrategy strategy, int modBitLength, ATPManager.Function priceEvaluation, CmdLineParser.obliviousTransferProtocol obliviousTransferProtocol) throws ParseException {
        super.withResourcePool(strategy, modBitLength, obliviousTransferProtocol);
        myManager.setPriceCalculation(priceEvaluation);
        return this;
    }

    /**
     * Network setup
     * @param parties   Map of parties according to input
     * @param myParty   My Party, as in the Party map
     * @return HostBuilder instance
     * @throws ParseException results, when inconsistencies are found in the input
     */
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
