package com.bwat.pendant.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bwat.pendant.*;

import java.util.Calendar;

/**
 * @author Kareem ElFaramawi
 *         NOTE: I'm marking everything relating to the speed stuff with SS, all this should be refactored into another class
 */
public class HomeFragment extends AGVFragment {
//	SocketConnection connection;

	SpringSeekBar speed; //SS
	SpringSeekBar heading; //SS
//	Timer speedThread;  //SS

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home_temp, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setStatus("DISCONNECTED");

		getView().findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onConnectClick(v);
			}
		});
//		getView().findViewById(R.id.commandSendButton).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				sendCommand();
//			}
//		});

		//SS
		speed = new SpringSeekBar((SeekBar) getView().findViewById(R.id.seekSpeed), -255, 255, 0);
		heading = new SpringSeekBar((SeekBar) getView().findViewById(R.id.seekHeading), -90, 90, 0);
//		getView().findViewById(R.id.updateMsButton).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				setSpeedUpdate();
//			}
//		});
//		setSpeedUpdate();

//		setCommandsEnabled(false);
	}

	public void onConnectClick(View view) {
		final String ipText = ((EditText) getView().findViewById(R.id.ipAddress)).getText().toString();
//		final String portText = ((EditText) getView().findViewById(R.id.vPort)).getText().toString();

		if (!NetUtils.isValidIPAddress(ipText)/* || !NetUtils.isValidPort(portText)*/) {
			Toast.makeText(getActivity(), "Invalid IP or Port", Toast.LENGTH_LONG).show();
			return;
		}

//		final int port = Integer.parseInt(portText);
		final int port = 1234;
		setStatus("CONNECTING");
		AGVUtils.logD("About to connect");
		getAGVActivity().connect(ipText, port);
		setStatus("CONNECTED");
		AGVUtils.logD("Should be connected now");
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					connection = new SocketConnection(ipText, port, new ConnectionListener() {
//						@Override
//						public void onConnect(Socket s) {
//							setStatus("CONNECTED");
//							setCommandsEnabled(true);
//						}
//
//						@Override
//						public void onDataReceived(String data) {
//							AGVUtils.logD( data);
//							displayResponse(data);
//						}
//
//						@Override
//						public void onDisconnect(Socket s) {
//							setStatus("DISCONNECTED");
//							setCommandsEnabled(false);
//							connection = null;
//						}
//					});
//					((AGVMainActivity) getActivity()).setConnection(connection);
//				} catch (final IOException e) {
//					setStatus("DISCONNECTED");
//					getActivity().runOnUiThread(new Runnable() {
//						@Override
//						public void run() {
//							Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
//						}
//					});
//				}
//			}
//		}.start();
	}

	void setStatus(final String status) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				((TextView) getView().findViewById(R.id.connectStatus)).setText(getString(R.string.status_start) + status);
			}
		});
	}

	void processResponse(final String msg) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (msg.contains("speed") || msg.contains("heading")) {
					String message = msg.substring(msg.indexOf(msg.contains("speed") ? "speed" : "heading"), msg.length());
					String[] tokens = message.split(" ");
					if (tokens.length > 2) {
						if (tokens[0].contains("speed")) {
							((TextView) getView().findViewById(R.id.updateSpeed)).setText(getString(R.string.updateSpeed) + tokens[2]);
						} else if (tokens[0].contains("heading")) {
							((TextView) getView().findViewById(R.id.updateHeading)).setText(getString(R.string.updateHeading) + tokens[2]);
						}
					}
				} else {
					displayResponse(msg);
				}
			}
		});
	}

	void displayResponse(final String message) {
		if(getAGVActivity() != null) {
			getAGVActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((TextView) getView().findViewById(R.id.responseTimeText)).setText(getString(R.string.res_time) + Calendar.getInstance().getTime().toString());
					((TextView) getView().findViewById(R.id.responseMessageText)).setText(getString(R.string.res_message) + message);
				}
			});
		}
	}

	@Override
	public void updateAGV(AGVConnection con) {
		AGVUtils.logD("Updating form home fragment");
		con.sendCommand("set speed " + speed.getProgress());
		con.sendCommand("set heading " + heading.getProgress());
		con.sendCommand("get speed");
		con.sendCommand("get heading");
	}

	@Override
	public void processAGVResponse(String message) {
//							AGVUtils.logD( data);
		displayResponse(message);
	}

	void sendCommand() {
		sendCommand(((EditText) getView().findViewById(R.id.commandText)).getText().toString());
	}

	void sendCommand(final String command) {
		getAGVActivity().getCon().sendCommand(command);
	}

//	void setCommandsEnabled(final boolean enabled) {
//		getActivity().runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				getView().findViewById(R.id.commandText).setEnabled(enabled);
//				getView().findViewById(R.id.commandSendButton).setEnabled(enabled);
//				getView().findViewById(R.id.seekSpeed).setEnabled(enabled);
//				getView().findViewById(R.id.seekHeading).setEnabled(enabled);
//			}
//		});
//	}

	//SS //SS //SS //SS //SS //SS //SS //SS //SS //SS //SS //SS //SS
//	void setSpeedUpdate() {
//		setSpeedUpdate(Long.parseLong(((EditText) getView().findViewById(R.id.updateMsText)).getText().toString()));
//	}

//	void setSpeedUpdate(long millis) {
//		if (speedThread != null) {
//			speedThread.cancel();
//			speedThread.purge();
//		}
//
//		speedThread = new Timer();
//		speedThread.scheduleAtFixedRate(new TimerTask() {
//			@Override
//			public void run() {
////				sendCommand("set speed " + speed.getProgress());
////				sendCommand("set heading " + heading.getProgress());
////				sendCommand("get speed");
////				sendCommand("get heading");
//			}
//		}, 1, millis);
//		((TextView) getView().findViewById(R.id.updateMsDisplay)).setText(getString(R.string.updateSpeedDisplay) + millis);
//	}
//}
}