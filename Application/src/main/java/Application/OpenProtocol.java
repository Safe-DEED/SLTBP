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

    public OpenProtocol(Map<Integer, Boolean> resultMap, Map<Integer, List<ATPManager.ATPUnit>> unitListMap, Map<Integer,
            DRes<SInt>> pricePerUnitMap){

        acceptedOrders = new ArrayList<>();
        for(Map.Entry<Integer, Boolean> entry : resultMap.entrySet()){
            int salesPos = entry.getKey();
            SecretDateHost.log("Entry " + entry.getKey() + " is " + entry.getValue());
            if(entry.getValue()){
                DRes<SInt> pricePerUnit = pricePerUnitMap.get(salesPos);
                List<ATPManager.ATPUnit> atpList = unitListMap.get(salesPos);
                if(pricePerUnit == null || atpList == null){
                    throw new RuntimeException("Order Succeeded while price or list is null - at Sales Position: " + salesPos);
                }
                for (ATPManager.ATPUnit unit: atpList) {
                    if(unit.id != 1){
                        unit.closedPrice = pricePerUnit;
                        acceptedOrders.add(unit);
                    }
                }
            }
        }
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        if(acceptedOrders.size() == 0){
            return builder.seq(seq -> () -> null);
        }
        return builder.seq(seq -> {
            SecretDateHost.log("Starting the open Protocol");
            for (ATPManager.ATPUnit unit: acceptedOrders) {
                unit.closedPrice = seq.numeric().mult(unit.closedPrice, unit.closedAmount);
            }
            return null;
        }).seq((seq, nil) -> {
            SecretDateHost.log("Open Values");
            for (ATPManager.ATPUnit unit: acceptedOrders) {
                ATPManager.instance.open(unit, 1, seq, true);
                DRes<BigInteger> openedPrice  =  seq.numeric().open(unit.closedPrice, unit.id);
                if(SecretDateHost.myID == unit.id){
                    unit.openedPrice = openedPrice;
                }
            }
            return null;
        });
    }

    @Override
    public void close() {
        Application.super.close();
    }

}
