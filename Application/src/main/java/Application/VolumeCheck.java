package Application;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VolumeCheck implements Application<BigInteger, ProtocolBuilderNumeric> {

    private final BigInteger volume;
    private DRes<SInt> hostAmount, totalClientVolume, result;
    private List<DRes<SInt>> clientAmounts;
    private final SecretDateHost secretDateHost;

    public VolumeCheck(SecretDateHost secretDateHost, BigInteger volume){
        this.volume = volume;
        this.secretDateHost = secretDateHost;
    }

    @Override
    public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
        return builder.seq(seq -> {
            Numeric num = seq.numeric();
            SecretDateHost.logger.info("Starting volume check");
            hostAmount = secretDateHost.myID == 1 ?
                    num.input(volume, 1) :
                    num.input(null, 1);
            clientAmounts = new ArrayList<>();
            for (Map.Entry<Integer, Party> entry : secretDateHost.myNetworkManager.getParties().entrySet()){
                int id = entry.getKey();
                if(id == 1) {
                    continue;
                }
                DRes<SInt> current_volume = secretDateHost.myID == id ?
                        num.input(volume, id) :
                        num.input(null, id);
                clientAmounts.add(current_volume);
            }
            return () -> null;
        }).seq((seq, nil) -> {
            totalClientVolume = seq.advancedNumeric().sum(clientAmounts);
            return () -> null;
        }).seq((seq, nil) -> {
            result = seq.comparison().compareLEQ(totalClientVolume, hostAmount);
            return () -> null;
        }).seq((seq, nil) -> seq.numeric().open(result));
    }

    @Override
    public void close() {
        secretDateHost.mySce.shutdownSCE();
    }
}