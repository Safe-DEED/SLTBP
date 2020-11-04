package utils;

import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class SocketNetwork implements Closeable, Network {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofMillis(100);
    private static final Logger logger = LoggerFactory.getLogger(SocketNetwork.class);
    private final BlockingQueue<byte[]> selfQueue;
    private final NetworkConfiguration conf;
    private boolean alive;
    private final Collection<Socket> sockets;
    private final Map<Integer, Sender> senders;
    private final Map<Integer, Receiver> receivers;

    /**
     * Utility class for handling SSL connections. In the earlier versions of FRESCO this functionality was set private,
     * so here we implemented the features we needed to make SSL work.
     * @param conf The network configuration given in the Network Config Json file
     * @param timeout The duration how long we wait for the network to be setup
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws KeyManagementException
     */
    public SocketNetwork(NetworkConfiguration conf, Duration timeout) throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        KeyManager[] kms = getKeyStoreManager(conf.getMyId());
        TrustManager[] tms = getTrustStoreManager();
        context.init(kms, tms, null);

        SSLSocketFactory socketFactory = context.getSocketFactory();
        SSLServerSocketFactory serverFactory = context.getServerSocketFactory();
        TLSConnector connector = new TLSConnector(conf, timeout, socketFactory, serverFactory);
        Map<Integer, Socket> socketMap = connector.getSocketMap();
        //logger.info("socketMaP: " + socketMap);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(socketMap);

        for (Map.Entry<Integer, Socket> entry : socketMap.entrySet()) {
            int i = entry.getKey();
            Socket s = entry.getValue();
            if (i == conf.getMyId()) {
                continue;
            }
            if (s.isClosed()) {
                throw new IllegalArgumentException("Closed socket for P" + i);
            }
            if (!s.isConnected()) {
                throw new IllegalArgumentException("Unconnected socket for P" + i);
            }
            try{
                s.setTcpNoDelay(true);
            } catch (SocketException e){
                throw new RuntimeException("Could not set delayless TCP connection");
            }

        }
        this.conf = conf;
        int externalParties = conf.noOfParties() - 1;
        this.receivers = new HashMap<>(externalParties);
        this.senders = new HashMap<>(externalParties);
        this.alive = true;
        this.selfQueue = new LinkedBlockingQueue<>();
        if (conf.noOfParties() > 1) {
            this.sockets = Collections.unmodifiableCollection(new ArrayList<>(socketMap.values()));
            startCommunication(socketMap);
        } else {
            this.sockets = Collections.emptyList();
        }
    }


    /**
     * Starts communication threads to handle incoming and outgoing messages.
     *
     * @param sockets a map from party ids to the associated communication channels
     */
    private void startCommunication(Map<Integer, Socket> sockets) {
        for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
            final int id = entry.getKey();
            //inRange(id);
            Socket socket = entry.getValue();
            Receiver receiver = new Receiver(socket);
            this.receivers.put(id, receiver);
            Sender sender = new Sender(socket);
            this.senders.put(id, sender);
        }
    }


    @Override
    public void send(int partyId, byte[] data) {
        if (partyId == conf.getMyId()) {
            this.selfQueue.add(data);
        } else {
            inRange(partyId, senders);
            if (!senders.get(partyId).isRunning()) {
                throw new RuntimeException(
                        "P" + conf.getMyId() + ": Unable to send to P" + partyId + ". Sender not running");
            }
            this.senders.get(partyId).queueMessage(data);
        }
    }

    @Override
    public byte[] receive(int partyId) {
        if (partyId == conf.getMyId()) {
            try {
                return selfQueue.take();
            } catch (InterruptedException e)
            {
                throw new RuntimeException("Received from self failed");
            }
        }
        inRange(partyId, receivers);
        byte[] data;
        data = receivers.get(partyId).pollMessage(RECEIVE_TIMEOUT);
        while (data == null) {
            if (!receivers.get(partyId).isRunning()) {
                throw new RuntimeException("P" + conf.getMyId() + ": Unable to recieve from P" + partyId
                        + ". Receiver not running");
            }
            data = receivers.get(partyId).pollMessage(RECEIVE_TIMEOUT);
        }
        return data;
    }

    @Override
    public int getNoOfParties() {
        return conf.noOfParties();
    }


    /**
     * Check if a party ID is in the range of known parties.
     *
     * @param partyId an ID for a party
     */
    private <T> void inRange(final int partyId, Map<Integer, T> map) {
        if (!map.containsKey(partyId)) {
            throw new IllegalArgumentException(
                    "Party " + partyId + " id not contained in the Map: " + map);
        }
    }



    /**
     * Safely closes the threads and channels used for sending/receiving messages. Note: this should
     * be only be called once.
     */
    private void closeCommunication() {
        for (Sender s : senders.values()) {
            s.stop();
        }
        for (Receiver r : receivers.values()) {
            r.stop();
        }
        for (Socket sock : sockets) {
            ExceptionConverter.safe(() -> {
                sock.close();
                return null;
            }, "Unable to properly close socket");
        }
    }


    @Override
    public void close() throws IOException {
        if (alive) {
            alive = false;
            if (conf.noOfParties() < 2) {
                return;
            }
            ExceptionConverter.safe(() -> {
                closeCommunication();
                return null;
            }, "Unable to properly close the network.");
        }
    }


    /**
     * Get truststore from the utils recources. At the moment, trust is precompiled into the application
     * @return TrustManagers for all parties
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    private TrustManager[] getTrustStoreManager() throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = classloader.getResourceAsStream("truststore")) {
            ks.load(is, "testpass".toCharArray());
        }
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        //logger.info("retrieved TrustStore: " + tmf);
        return tmf.getTrustManagers();
    }

    /**
     * Key key from utils resources.
     * @param id The id of the party according to the network config
     * @return Returns the key manager according to the key of the party
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */

    private KeyManager[] getKeyStoreManager(int id) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = classloader.getResourceAsStream("keystore" + id)) {
            ks.load(is, "testpass".toCharArray());
        }
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "testpass".toCharArray());
        //logger.info("retrieved keyStore: " + kmf);
        return kmf.getKeyManagers();
    }
}
