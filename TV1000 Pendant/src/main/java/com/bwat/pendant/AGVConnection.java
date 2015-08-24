package com.bwat.pendant;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Kareem ElFaramawi
 */
public class AGVConnection implements ConnectionListener {
	public interface UpdateListener {
		void update();
	}

	private static AGVConnection INSTANCE = null;
	SocketConnection connection = null;
	ConnectionListener listener = this;


	Timer updateThread = null;
	UpdateListener updater = null;
	long updateDelay = 250;

	private AGVConnection() {
//		setUpdateDelay(updateDelay); //default update speed
	}

	public static AGVConnection getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new AGVConnection();
		}
		return INSTANCE;
	}

	/**
	 * Connects to a socket with a given listener
	 *
	 * @param host     IP address of socket
	 * @param port     Socket port
	 * @param listener Listener for callback actions of connection. Saved for later connections
	 */
	public void connect(String host, int port, ConnectionListener listener) {
		this.listener = listener;
		connect(host, port);
	}

	/**
	 * Connects to a socket using the default port of 1234 and a saved listener
	 *
	 * @param host IP address of socket
	 */
	public void connect(String host) {
		connect(host, 1234, listener);
	}

	/**
	 * Connects to a socket with a saved listener
	 *
	 * @param host IP address of socket
	 * @param port Socket port
	 */
	public void connect(final String host, final int port) {
		new Thread() {
			@Override
			public void run() {
				try {
					connection = new SocketConnection(host, port, listener);
					setUpdateDelay(updateDelay);
				} catch (IOException e) {
					listener.onDisconnect(null);
					AGVUtils.logE(e.getMessage());
				}
			}
		}.start();
	}

	public void disconnect() {
		killUpdateThread();
		if (isConnected()) {
			connection.disconnect();
			connection = null;
		}
	}

	public void sendCommand(final String command) {
		new Thread() {
			@Override
			public void run() {
				if (isConnected()) {
					connection.send(command);
				}
			}
		}.start();
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	public void setUpdateListener(UpdateListener updater) {
		this.updater = updater;
	}

	public long getUpdateDelay() {
		return updateDelay;
	}

	public void setUpdateDelay(long delay) {
		updateDelay = delay;
		killUpdateThread();

		updateThread = new Timer();
		updateThread.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (updater != null) {
					updater.update();
				}
			}
		}, 1, updateDelay);
	}

	private void killUpdateThread() {
		if (updateThread != null) {
			updateThread.cancel();
			updateThread.purge();
		}
	}

	public String getHost() {
		return connection.getHost();
	}

	public int getPort() {
		return connection.getPort();
	}

	//Dummy methods for a listener in case one is never provided
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
