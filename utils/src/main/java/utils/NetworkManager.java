package utils;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The NetworkManager enables multiple networks with the same participants to be managed.
 */
public class NetworkManager implements Closeable {

    private static Logger log = LoggerFactory.getLogger(NetworkManager.class);
    private final AtomicInteger PORT_OFFSET_COUNTER = new AtomicInteger(0);
    private final int PORT_INCREMENT = 20;
    private final Map<Integer, Network> openedNetworks;
    private final Map<Integer, Party> partyMap;
    private final NetworkConfiguration configuration;
    private int portOffset;
    private final boolean logging;

    /**
     * Create a new NetworkManager
     * @param configuration the configuration
     * @param logging whether this application uses logging
     * @param parties the parties with which the networks are created
     */
    public NetworkManager(NetworkConfiguration configuration, boolean logging, Map<Integer, Party> parties) {
        this.portOffset = 0;
        this.openedNetworks = new HashMap<>();
        this.configuration = configuration;
        this.logging = logging;
        this.partyMap = parties;
        log("Created NetworkManager");
    }

    private void log(String string){
        if(logging){
            log.info(string);
        }
    }
    //

    /**
     * create new Network configuration by incrementing the ports
     * @return the new network configuration
     */
    private NetworkConfiguration UpdateConfiguration(){
        this.portOffset = PORT_OFFSET_COUNTER.addAndGet(partyMap.size());
        Map<Integer, Party> parties = new HashMap<>();
        for(Map.Entry<Integer, Party> entry : partyMap.entrySet()){
            int i = entry.getKey();
            Party p = configuration.getParty(i);
            //log.info("adding party:" + p.getPartyId() + " at: " + i + " with port: " + p.getPort());
            parties.put(i, new Party(i, p.getHostname(), p.getPort() + portOffset));
        }
        return new NetworkConfigurationImpl(configuration.getMyId(), parties);
    }

    /**
     * Create another network with the same parties but different ports, just to not interfere with the other protocols
     * using the same network.
     * @return the newly created network
     * @param caller
     */
    public Network createExtraNetwork(String caller){
        // log.info("creating extra network");
        NetworkConfiguration conf = UpdateConfiguration();
        log("config: " + configuration.noOfParties());
        Network net;
        try {
            net = new utils.SocketNetwork(conf, Duration.ofMinutes(10));
            log("created extra network");
        } catch (Exception e) {
            log.info("Failed creating new network: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
        if(logging){
            net = new NetworkLoggingDecorator(net);
        }
        openedNetworks.put(portOffset, net);
        return net;
    }


    public Map<Integer, Party> getParties(){
        return partyMap;
    }

    /**
     * Compare two parties with each other
     * @param p1 party 1
     * @param p2 party 2
     * @return is p1 == p2? (true/false)
     */
    public static boolean equalParties(Party p1, Party p2){
        boolean equalHost = p1.getHostname().equals(p2.getHostname());
        boolean equalPort = p1.getPort() == p2.getPort();
        boolean equalID   = p1.getPartyId() == p2.getPartyId();
        //log.info("comparing: " + p1 + " with " + p2 + " returned: " + equalHost && equalID && equalPort);
        return equalHost && equalID && equalPort;
    }

    /**
     * in a multithreaded setting there are several parties with the same id...
     * Returns that party map, which contains my party instance
     * @param partyList the list of all party maps
     * @param myParty the instance of my party object
     * @return the party map which this party belongs to
     */
    public static Map<Integer, Party> getPartyMap(List<Map<Integer, Party>> partyList, Party myParty) {
        for (Map<Integer, Party> partyMap : partyList) {
            for (Map.Entry<Integer, Party> entry : partyMap.entrySet()) {
                if (equalParties(entry.getValue(), myParty)) {
                    return partyMap;
                }
            }
        }
        return null;
    }

    /**
     * If logging is activated, returns all the logged values from all networks
     * this is used to gather the amount of network traffic received from all the parties
     * @return The string map of all the loggings
     */
    public Map<String, Long> getLoggedValues(){

        if(!logging){
            return null;
        }

        Map<String, Long> values = new HashMap<>();
        long totalNoBytes = 0;
        long noNetworkBatches = 0;
        for( Map.Entry<Integer, Network> entry : openedNetworks.entrySet()){
            Map<String, Long> logvalues = ((NetworkLoggingDecorator)entry.getValue()).getLoggedValues();
            totalNoBytes += logvalues.get(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES);
            noNetworkBatches += logvalues.get(NetworkLoggingDecorator.NETWORK_TOTAL_BATCHES);
        }
        values.put(NetworkLoggingDecorator.NETWORK_TOTAL_BYTES, totalNoBytes);
        values.put(NetworkLoggingDecorator.NETWORK_TOTAL_BATCHES, noNetworkBatches);
        return values;
    }

    /**
     * closes the networkManager and all the networks
     */
    @Override
    public void close() {
        log.info("Closing the network manager!");
        openedNetworks.forEach((key, value) -> close((Closeable) value));
    }

    /**
     * closes a specific network
     * @param closeable the network to be closed
     */
    private void close(Closeable closeable) {
        ExceptionConverter.safe(() -> {
            closeable.close();
            return null;
        }, "IO Exception");
    }

}
