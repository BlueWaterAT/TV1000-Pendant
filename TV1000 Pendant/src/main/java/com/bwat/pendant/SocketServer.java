package com.bwat.pendant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Socket server that listens for connection over a certain port on the local network. The socket can send
 * or receive messages with clients, and has the ability to send a message and block until a response is
 * received. All messages sent through this socket must end in a newline ('\n').
 *
 * @author Kareem El-Faramawi
 */
public class SocketServer {
    Logger log = LoggerFactory.getLogger(getClass());

    public enum Event {
        CONNECT,
        DATA,
        DISCONNECT
    }

    public interface EventCallback {
        public void call(Event type, Object... data);
    }

    // String that signals the server to disconnect from the client that sent it
    public static final String MSG_DISC = "!DC";

    // The port to listen on
    private int port;
    // The socket that will be listening for connections
    private ServerSocket server;
    // List of all connections
    private ArrayList<Connection> clients = new ArrayList<Connection>();
    // Map of all event callbacks
    private HashMap<Event, ArrayList<EventCallback>> events = new HashMap<Event, ArrayList<EventCallback>>();

    /**
     * Creates a SocketServer that listens over a given port
     *
     * @param port Port to listen on
     * @throws java.io.IOException
     */
    public SocketServer(int port) throws IOException {
        this.port = port;
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));
        // Listen for connections in a separate thread
        new Thread() {
            public void run() {
                while (true) {
                    if (server.isClosed()) {
                        break;
                    }
                    try {
                        // Accept a connection and add it to the master list
                        Socket client = server.accept();
                        clients.add(new Connection(client));
                        trigger(Event.CONNECT, clients.get(clients.size() - 1));
                        log.debug("Client " + client + "connected");
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                    Thread.yield();
                }
            }
        }.start();
        for (Event e : Event.values()) {
            events.put(e, new ArrayList<EventCallback>());
        }
    }

    /**
     * @return The port this server is listening on
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The number of connected clients
     */
    public int getNumConnections() {
        return clients.size();
    }

    /**
     * Disconnects from all clients that connected to this server
     */
    public void disconnectAll() {
        while (!clients.isEmpty()) {
            clients.get(0).disconnect();
        }
        clients.clear();
    }

    public void close() throws IOException {
        disconnectAll();
        server.close();
    }

    public void on(Event event, EventCallback callback) {
        if (callback != null) {
            events.get(event).add(callback);
        }
    }

    public void trigger(Event event, Object... data) {
        for (EventCallback ec : events.get(event)) {
            ec.call(event, data);
        }
    }

    /**
     * Sends a String message to all connected clients. If waitForResponse is true, this will return an array
     * of all responses received from the clients.
     *
     * @param message         Message to send to clients
     * @param waitForResponse If true, the server will wait after sending each message for a response before
     *                        moving on
     * @return Array of all responses from connected clients. If waitForResponse is false, this will be full
     * of empty strings
     */
    public String[] sendAll(String message, boolean waitForResponse) {
        log.debug("Sending string \"" + message + "\" to all clients");
        String[] responses = new String[clients.size()];
        for (int i = 0; i < clients.size(); i++) {
            responses[i] = clients.get(i).send(message, waitForResponse);
        }
        return responses;
    }

    /**
     * Sends a byte message to all connected clients. If waitForResponse is true, this will return an array
     * of all responses received from the clients.
     *
     * @param b               byte to send to clients
     * @param waitForResponse If true, the server will wait after sending each byte for a response before
     *                        moving on
     * @return Array of all responses from connected clients. If waitForResponse is false, this will be full
     * of empty strings
     */
    public String[] sendAll(byte b, boolean waitForResponse) {
        String[] responses = new String[clients.size()];
        for (int i = 0; i < clients.size(); i++) {
            responses[i] = clients.get(i).send(b, waitForResponse);
        }
        return responses;
    }

    /**
     * Sends a byte[] message to all connected clients. If waitForResponse is true, this will return an array
     * of all responses received from the clients.
     *
     * @param byteArray       Message to send to clients
     * @param waitForResponse If true, the server will wait after sending each byte[] for a response before
     *                        moving on
     * @return Array of all responses from connected clients. If waitForResponse is false, this will be full
     * of empty strings
     */
    public String[] sendAll(byte[] byteArray, boolean waitForResponse) {
        String[] responses = new String[clients.size()];
        for (int i = 0; i < clients.size(); i++) {
            responses[i] = clients.get(i).send(byteArray, waitForResponse);
        }
        return responses;
    }

    /**
     * Represents a connection to a single remote Socket. This has its own thread to handle communication with
     * this Socket
     *
     * @author Kareem El-Faramawi
     */
    public class Connection {
        // Connected Socket
        protected Socket socket;
        // Input stream from Socket
        private BufferedReader reader;
        // Output stream from Socket
        private PrintWriter writer;
        // If this connection is waiting for a response
        private volatile boolean waiting = false;
        // The last received response from the connected client
        private volatile String response = "";
        // Flag for the input thread
        private volatile boolean running;

        /**
         * Creates a Connection to the given Socket and starts a new Thread to handle I/O
         *
         * @param sock Socket to connect to
         * @throws java.net.UnknownHostException
         * @throws java.io.IOException
         */
        public Connection(Socket sock) throws UnknownHostException, IOException {
            this.socket = sock;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            // Create a new thread to handle receiving message from the Socket
            running = true;
            new Thread() {
                boolean reachable = true;

                public void run() {
                    while (running) {
                        if (!socket.isConnected() || socket.isClosed() || !reachable) {
                            disconnect();
                            break;
                        }
                        try {
                            response = reader.readLine();
                            if (response == null || response.equals(MSG_DISC)) {
                                disconnect();
                                break;
                            }
                            trigger(Event.DATA, response, this);
                            log.debug("Message received: " + response);
                            waiting = false;
                        } catch (IOException e) {
                            log.debug("Error reading from input: " + e.getMessage());
                        }
                        Thread.yield();
                    }
                }
            }.start();
        }

        /**
         * Disconnect from the Socket and close all streams
         */
        public void disconnect() {
            try {
                running = false;
                send(MSG_DISC, false);
                trigger(Event.DISCONNECT, this);
                reader.close();
                writer.close();
                socket.close();
                clients.remove(this);
                log.debug(socket + " disconnected");
            } catch (IOException e) {
                log.debug("Error disconnecting: " + e.getMessage());
            }
        }

        /**
         * Send a String message to this Socket.
         *
         * @param message         Message to be sent
         * @param waitForResponse If true, this will wait until a response is received, otherwise ignores any
         *                        response
         * @return If waiting for a response, it will be returned, otherwise a blank String is returned;
         */
        public String send(String message, boolean waitForResponse) {
            // Send message
            writer.println(message);
            writer.flush();
            log.debug("String \"" + message + "\" sent to " + socket);
            if (waitForResponse) {
                waiting = true;
                log.debug("Waiting for response from: " + socket);
                // Wait until response is received
                while (waiting) {
                    Thread.yield();
                }
                return response;
            }
            return "";
        }

        /**
         * Send a byte to this Socket.
         *
         * @param b               byte to be sent
         * @param waitForResponse If true, this will wait until a response is received, otherwise ignores any
         *                        response
         * @return If waiting for a response, it will be returned, otherwise a blank String is returned;
         */
        public String send(byte b, boolean waitForResponse) {
            return send(new byte[]{b}, waitForResponse);
        }

        /**
         * Send a byte[] to this Socket.
         *
         * @param byteArray       byte[] to be sent
         * @param waitForResponse If true, this will wait until a response is received, otherwise ignores any
         *                        response
         * @return If waiting for a response, it will be returned, otherwise a blank String is returned;
         */
        public String send(byte[] byteArray, boolean waitForResponse) {
            return send(new String(byteArray), waitForResponse);
        }
    }
}
