package com.bwat.pendant;

import java.net.Socket;

/**
 * @author Kareem ElFaramawi
 */
public interface ConnectionListener {
	public void onConnect(Socket s);
	public void onDataReceived(String data);
	public void onDisconnect(Socket s);
}
