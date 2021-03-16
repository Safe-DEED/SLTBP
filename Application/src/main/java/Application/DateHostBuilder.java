package Application;

import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import utils.MPCBuilder;

public class DateHostBuilder extends MPCBuilder<SecretDateHost> {

    private SecretDateHost.EvaluationProtocol protocol = SecretDateHost.EvaluationProtocol.LINEAR;

    /**
     * Creating the builder following the builder pattern.
     *
     * @param logging whether logging is activated during the computation
     */
    public DateHostBuilder(boolean logging) {
        super(logging);
    }

    public DateHostBuilder withProtocol(SecretDateHost.EvaluationProtocol protocol){
        this.protocol = protocol;
        return this;
    }


    @Override
    public SecretDateHost build() {
        BatchedProtocolEvaluator<SpdzResourcePool> evaluator = new BatchedProtocolEvaluator<>(batchEvalStrat, mySuite, 4096);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce = new SecureComputationEngineImpl<>(mySuite, evaluator);
        SecretDateHost host = new SecretDateHost();
        host.myID             = myID;
        host.maxBitLength     = maxBitLength;
        host.numParties       = numberOfParties;
        host.myVolume        = myVolume;
        host.minPrice         = myPrice;
        host.myNetwork        = myNetwork;
        host.mySce            = mySce;
        host.myDate           = myDate;
        host.myPool           = myPool;
        host.logging          = logging;
        host.myNetworkManager = myNetworkManager;
        host.myManager        = myManager;
        host.units            = units;
        host.protocol         = protocol;
        if(protocol == SecretDateHost.EvaluationProtocol.LINEAR){
            host.priceProtocol = new LinearProtocol();
        } else {
            host.priceProtocol = null;
        }
        return host;
    }
}
