package utils;

import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.logging.PerformanceLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A decorator for the network to extract the logged data to the network manager
 */
public class NetworkLoggingDecorator implements Network, PerformanceLogger, Closeable {

    public static final String NETWORK_PARTY_BYTES = "Amount of bytes received pr. party";
    public static final String NETWORK_TOTAL_BYTES = "Total amount of bytes received";
    public static final String NETWORK_TOTAL_BATCHES = "Total amount of batches received";

    private Network delegate;
    private Map<Integer, PartyStats> partyStatsMap = new HashMap<>();

    /**
     * creates the decorator for the given network
     * @param network the delegate network to be decorated
     */
    public NetworkLoggingDecorator(Network network) {
        this.delegate = network;
    }

    /**
     * Upon receiving from a party, the received bytes are stored in a map
     * @param partyId the party from which to receive
     * @return the received bytes
     */
    @Override
    public byte[] receive(int partyId) {
        byte[] res = this.delegate.receive(partyId);
        int noBytes = res.length;
        partyStatsMap.computeIfAbsent(partyId, (i) -> new PartyStats()).recordTransmission(noBytes);
        return res;
    }

    @Override
    public int getNoOfParties() {
        return delegate.getNoOfParties();
    }

    @Override
    public void send(int partyId, byte[] data) {
        this.delegate.send(partyId, data);
    }

    @Override
    public void reset() {
        partyStatsMap.clear();
    }

    @Override
    public void close() throws IOException {
        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        }
    }

    /**
     * The data structure to store information about received transmissions
     */
    private class PartyStats {
        private long count;
        private long noBytes;

        /**
         * Upon receiving information, record transmission is called
         * @param noBytes the number of bytes received
         */
        public void recordTransmission(int noBytes) {
            this.count++;
            this.noBytes += noBytes;
        }
    }

    /**
     * get the logged values
     * @return Return the entire map of party stats - used in the network Manager
     */
    @Override
    public Map<String, Long> getLoggedValues() {
        Map<String, Long> values = new HashMap<>();

        long totalNoBytes = 0;
        long noNetworkBatches = 0;
        for (Integer partyId : partyStatsMap.keySet()) {
            PartyStats partyStats = partyStatsMap.get(partyId);
            values.put(NETWORK_PARTY_BYTES + "_" + partyId, partyStats.noBytes);
            totalNoBytes += partyStats.noBytes;
            noNetworkBatches += partyStats.count;
        }
        values.put(NETWORK_TOTAL_BYTES, totalNoBytes);
        values.put(NETWORK_TOTAL_BATCHES, noNetworkBatches);
        return values;
    }

}
