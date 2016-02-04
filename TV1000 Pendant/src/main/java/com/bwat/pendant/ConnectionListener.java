package com.bwat.pendant;

import java.net.Socket;

/**
 * @author Kareem ElFaramawi
 */
public interface ConnectionListener {
    void onConnect(Socket s);

    void onDataReceived(String data);

    void onDisconnect(Socket s);
}
