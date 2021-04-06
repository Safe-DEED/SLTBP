package Application;

import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import utils.MPCBuilder;

public class DateHostBuilder extends MPCBuilder<SecretDateHost> {

    private SecretDateHost.EvaluationProtocol protocol = SecretDateHost.EvaluationProtocol.LINEAR;
    private PriceProtocol priceProtocol;

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
        switch (protocol){
            case LINEAR:
                priceProtocol = new LinearProtocol();
                break;
            case BUCKET:
                priceProtocol = new BucketProtocol();
                break;
            case CONVEX:
                priceProtocol = new ConvexProtocol();
                break;
            case CONCAVE:
                priceProtocol = new ConcaveProtocol();
                break;
        }
        return this;
    }


    @Override
    public SecretDateHost build() {
        BatchedProtocolEvaluator<SpdzResourcePool> evaluator = new BatchedProtocolEvaluator<>(batchEvalStrat, mySuite, 4096);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> mySce = new SecureComputationEngineImpl<>(mySuite, evaluator);
        SecretDateHost host = new SecretDateHost();
        if(debug){
            SecretDateHost.logger.warn("Care - insecure debug protocol activated!");
            SecretDateHost.logger.info("Care - insecure debug protocol activated!");
            SecretDateHost.logger.error("Care - insecure debug protocol activated!");
        }
        host.debug            = debug;
        host.myID             = myID;
        host.maxBitLength     = maxBitLength;
        host.numParties       = numberOfParties;
        host.myNetwork        = myNetwork;
        host.mySce            = mySce;
        host.myPool           = myPool;
        host.logging          = logging;
        host.myNetworkManager = myNetworkManager;
        host.myManager        = myManager;
        host.units            = units;
        host.protocol         = protocol;
        host.priceProtocol    = priceProtocol;
        return host;
    }
}
