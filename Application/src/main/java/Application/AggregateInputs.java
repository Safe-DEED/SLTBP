package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ProtocolBuilder;
import dk.alexandra.fresco.framework.network.Network;
import utils.ATPManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateInputs implements Application {

    private List<ATPManager.ATPUnit> units;
    private Map<Integer, List<ATPManager.ATPUnit>> salesPositionUnitMap;
    private Network network;

    public AggregateInputs(List<ATPManager.ATPUnit> units, Network network){
        this.units = units;
        this.network = network;
        salesPositionUnitMap = new HashMap<>();
        for(ATPManager.ATPUnit unit : units){
            if(salesPositionUnitMap.containsKey(unit.salesPosition)){
                continue;
            }
            salesPositionUnitMap.put(unit.salesPosition, new ArrayList<>());
        }
    }

    @Override
    public DRes buildComputation(ProtocolBuilder builder) {
        return null;
    }

    @Override
    public void close() {

    }
}
