package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ProtocolBuilder;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import utils.ATPManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenProtocol implements Application<BigInteger, ProtocolBuilderNumeric> {
    List<ATPManager.ATPUnit> acceptedOrders;

    public OpenProtocol(Map<Integer, Boolean> resultMap, Map<Integer, List<ATPManager.ATPUnit>> unitListMap){
        acceptedOrders = new ArrayList<>();
        for(Map.Entry<Integer, Boolean> entry : resultMap.entrySet()){
            int salesPos = entry.getKey();
            if(entry.getValue()){
                List<ATPManager.ATPUnit> atpList = unitListMap.get(salesPos);
                for (ATPManager.ATPUnit unit: atpList) {
                    if(unit.id != 1){
                        acceptedOrders.add(unit);
                    }
                }
            }
        }
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(acceptedOrders.size() == 0){
            return null;
        }
        return builder.seq(seq -> {
            SecretDateHost.log("Starting the open Protocol");
            for (ATPManager.ATPUnit unit: acceptedOrders) {
                ATPManager.instance.open(unit, 1, seq, true);
            }
            return null;
        });
    }

    @Override
    public void close() {
        Application.super.close();
    }

}
