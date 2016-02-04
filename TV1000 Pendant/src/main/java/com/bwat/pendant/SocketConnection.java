package com.bwat.pendant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketConnection implements ConnectionListener {
    private Socket socket;
    private String host;
    private int port;
    private BufferedReader reader;
    private PrintWriter writer;
    private ConnectionListener listener;

    public static final String MSG_DISC = "!DC";

    private long lastMessageTime = System.currentTimeMillis();
    static final long MESSAGE_TIMEOUT = 5000;
    static final long CONNECT_TIMEOUT = 2000;

    public SocketConnection(String host, int port, final ConnectionListener listener) throws UnknownHostException, IOException {
        this.host = host;
        this.port = port;
        this.listener = listener != null ? listener : this;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream());
        listener.onConnect(socket);

        new Thread() {
            public void run() {
                while (true) {
                    if (!socket.isConnected() || socket.isClosed()) {
                        break;
                    }
                    try {
                        if (reader.ready()) {
                            //Read a message from the stream
                            String message = reader.readLine();
                            lastMessageTime = System.currentTimeMillis();
                            //Disconnect if the server disconnected or failed to send a proper message
                            if (message == null || message.equals(MSG_DISC)) {
                                disconnect();
                                break;
                            }

                            //Pass the message on to the listener
                            listener.onDataReceived(message);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading from input: " + e.getMessage());
                    }
                    Thread.yield();
                }
            }
        }.start();
    }

    public void disconnect() {
        try {
            send(MSG_DISC);
            socket.close();
            reader.close();
            writer.close();
            listener.onDisconnect(socket);
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public void send(String message) {
        writer.println(message);
        writer.flush();
    }

    public void send(byte b) {
        writer.print(b);
        writer.flush();
    }

    public void send(byte[] byteArry) {
        writer.print(byteArry);
        writer.flush();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && System.currentTimeMillis() - lastMessageTime < MESSAGE_TIMEOUT;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    //Dummy implementation of connection listener
    @Override
    public void onConnect(Socket s) {
    }

    @Override
    public void onDataReceived(String data) {
    }

    @Override
    public void onDisconnect(Socket s) {
    }
}
