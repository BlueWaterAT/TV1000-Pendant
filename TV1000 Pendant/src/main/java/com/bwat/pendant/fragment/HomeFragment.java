package com.bwat.pendant.fragment;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bwat.pendant.AGVConnection;
import com.bwat.pendant.R;
import com.bwat.pendant.SpringSeekBar;
import com.bwat.pendant.bugstick.Joystick;
import com.bwat.pendant.bugstick.JoystickListener;
import com.bwat.pendant.lib.SpaceTokenizer;
import com.bwat.pendant.util.NetUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.http.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;


/**
 * @author Kareem ElFaramawi
 *         NOTE: I'm marking everything relating to the speed stuff with SS, all this should be refactored into another class
 */
public class HomeFragment extends AGVFragment {
    Logger log = LoggerFactory.getLogger(getClass());


    private final static String PROJECTS_DIR = Environment.getExternalStorageDirectory().getPath() + "/AGV Programmer";
    //FTP related variables
    private final static int FTP_PORT = 22;
    private final static String FTP_USER = "root";
    private final static String FTP_PASS = "bwat1234";
    private final static String FTP_REMOTE_DIR = "/hmi/";
    public int ButtonState = 0;

    public float degree;
    public float offset;

    public String ip;

    final Random rand = new Random();
    public int accesscode;
    public int accesstoguide;
    EditText myEditText;
    //	SocketConnection connection;

    SpringSeekBar speed; //SS
    SpringSeekBar heading; //SS
    //	Timer speedThread;  //SS
    MultiAutoCompleteTextView text1;
    String[] words = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setStatus("DISCONNECTED");
        if (getAGVActivity().getCon().isConnected()) {
            setStatus("CONNECTED");
        }


        File ip_address = new File(String.format("%s/%s/", PROJECTS_DIR, "IP_Address"));
        if (!ip_address.exists()) {
            ip_address.mkdirs();
        }
        FileWriter writer = null;
        File ipaddress = new File(ip_address, "ip_address.txt");
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(ipaddress));
            ByteArrayBuffer baf = new ByteArrayBuffer(200000);
            int current = 0;
            while ((current = in.read()) != -1) {
                baf.append((byte) current);
                //}
                byte[] myData = baf.toByteArray();
                String dataInString = new String(myData);
                words = dataInString.split(" ");
                text1 = (MultiAutoCompleteTextView) getView().findViewById(R.id.ipAddress);

                ArrayAdapter adapter = new ArrayAdapter(this.getAGVActivity(), android.R.layout.simple_list_item_1, words);
                text1.setAdapter(adapter);
                text1.setTokenizer(new SpaceTokenizer());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        getView().findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectClick(v);
            }
        });
        getView().findViewById(R.id.accessButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAccessClick(v);
            }
        });
        getView().findViewById(R.id.disconnectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectClick(v);
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

        final TextView angleView = (TextView) getView().findViewById(R.id.tv_angle);
        final TextView offsetView = (TextView) getView().findViewById(R.id.tv_offset);
        final String angleNoneString = getString(R.string.angle_value_none);
        final String angleValueString = getString(R.string.angle_value);
        final String offsetNoneString = getString(R.string.offset_value_none);
        final String offsetValueString = getString(R.string.offset_value);


        Joystick joystick = (Joystick) getView().findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
            }

            @Override
            public void onDrag(float degrees, float offset) {


                //                AGVConnection.getInstance().sendCommand("set speed " + offset*255);
                //				float newDegree;
                //				newDegree=90-degrees;
                //				if(newDegree>90&&newDegree<180)newDegree=90;
                //				if(newDegree>180)newDegree=-90;
                //				AGVConnection.getInstance().sendCommand("set heading " + newDegree);
                //				AGVConnection.getInstance().sendCommand("get speed");
                //				AGVConnection.getInstance().sendCommand("get heading");


                AGVConnection.getInstance().sendCommand("set speed " + offset * 255);
                float newDegree;
                newDegree = 90 - degrees;
                if (newDegree > 0 && newDegree < 90) newDegree = -newDegree;
                else if (newDegree < 0 && newDegree > -90) newDegree = -newDegree;
                else if (newDegree > 180) newDegree = 90;
                else if (newDegree < 180 && newDegree > 90) newDegree = -90;
                AGVConnection.getInstance().sendCommand("set heading " + newDegree);
                AGVConnection.getInstance().sendCommand("get speed");
                AGVConnection.getInstance().sendCommand("get heading");

                angleView.setText(String.format(angleValueString, newDegree));
                offsetView.setText(String.format(offsetValueString, offset * 255));

            }

            @Override
            public void onUp() {
                angleView.setText(angleNoneString);
                offsetView.setText(offsetNoneString);

            }
        });


        myEditText = ((EditText) getView().findViewById(R.id.accesscode));
        myEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        Editable myText = myEditText.getText();
                        String access = myText.toString();
                        String code = accesscode + "";
                        if (!access.equals(code)) {
                            getView().findViewById(R.id.connectButton).setBackgroundColor(Color.RED);
                            accesstoguide = 0;
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        Editable myText = myEditText.getText();
                        String access = myText.toString();
                        String code = accesscode + "";
                        if (access.equals(code)) {

                            if (accesstoguide == 1) {
                                getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_green));
                                Animation mAnimation = new AlphaAnimation(1, 0.5f);
                                mAnimation.setDuration(2000);
                                mAnimation.setInterpolator(new LinearInterpolator());
                                mAnimation.setRepeatCount(Animation.INFINITE);
                                mAnimation.setRepeatMode(Animation.REVERSE);
                                getView().findViewById(R.id.connectButton).startAnimation(mAnimation);
                            } else {
                                getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_blue));
                                accesstoguide = 1;
                                Toast.makeText(getActivity(), "Connection get ready, please type connect button for the manual control.", Toast.LENGTH_LONG).show();

                            }


                        } else {
                            getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_red));
                            accesstoguide = 0;
                            //
                            // Toast.makeText(getActivity(), "Invalid accesscode", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

        );


    }


    public void onAccessClick(View view) {

        accesstoguide = 0;
        view.clearAnimation();
        getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_red));
        //Connect to the controller over FTP and send txt file
        if (getAGVActivity().getCon().isConnected()) {
            sendAccesscode(getAGVActivity().getCon().getHost());
        } else {
            //			 AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
            //			 alert.setTitle("No Controller Connected");
            //			 alert.setMessage("Manually enter SFTP IP");
            //			 final EditText input = new EditText(getAGVActivity());
            //			 alert.setView(input);
            //			 alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            //				 @Override
            //				 public void onClick(DialogInterface dialog, int which) {
            //					 sendAccesscode(input.getText().toString());
            //				 }
            //			 });
            //			 alert.setNegativeButton("Cancel", null);
            //			 alert.show();
            Toast.makeText(getActivity(), "Connection get lost, please type connect button to connect.", Toast.LENGTH_LONG).show();

        }


    }


    public void onConnectClick(View view) {

        final String ipText = ((AutoCompleteTextView) getView().findViewById(R.id.ipAddress)).getText().toString();
        //		final String portText = ((EditText) getView().findViewById(R.id.vPort)).getText().toString();

        if (!NetUtils.isValidIPAddress(ipText)/* || !NetUtils.isValidPort(portText)*/) {
            Toast.makeText(getActivity(), "Invalid IP or Port", Toast.LENGTH_LONG).show();
            return;
        }

        //now we need create a file to store the ip address we typed
        File ip_address = new File(String.format("%s/%s/", PROJECTS_DIR, "IP_Address"));
        if (!ip_address.exists()) {
            ip_address.mkdirs();
        }


        FileWriter writer = null;
        File ipaddress = new File(ip_address, "ip_address.txt");
        if (!ipaddress.exists()) {
            try {
                writer = new FileWriter(ipaddress, true);
                writer.append(ipText + " ");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(ipaddress));
            ByteArrayBuffer baf = new ByteArrayBuffer(200000);
            int current = 0;
            while ((current = in.read()) != -1) {
                baf.append((byte) current);
            }
            byte[] myData = baf.toByteArray();
            String dataInString = new String(myData);
            words = dataInString.split(" ");
            int array_length = words.length;
            int i;
            //this loop is avoid the duplicate input
            for (i = 0; i < array_length; i++) {
                if (words[i].equals(ipText)) {
                    break;
                }
                if (i == array_length - 1) {
                    writer = new FileWriter(ipaddress, true);
                    writer.append(ipText + " ");
                    writer.flush();
                    writer.close();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            in = new BufferedInputStream(new FileInputStream(ipaddress));
            ByteArrayBuffer baf = new ByteArrayBuffer(200000);
            int current = 0;
            while ((current = in.read()) != -1) {
                baf.append((byte) current);
            }
            byte[] myData = baf.toByteArray();
            String dataInString = new String(myData);
            words = dataInString.split(" ");


            text1 = (MultiAutoCompleteTextView) getView().findViewById(R.id.ipAddress);
            ArrayAdapter adapter = new ArrayAdapter(this.getAGVActivity(), android.R.layout.simple_list_item_1, words);
            text1.setAdapter(adapter);
            text1.setTokenizer(new SpaceTokenizer());
        } catch (IOException e) {
            e.printStackTrace();
        }


        //		final int port = Integer.parseInt(portText);
        final int port = 1234;
        setStatus("CONNECTING");
        log.debug("About to connect");
        getAGVActivity().connect(ipText, port);
        ip = ipText;
        //setStatus("CONNECTED");
        log.debug("Should be connected now");


        if (accesstoguide == 1) {
            getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_green));
            Animation mAnimation = new AlphaAnimation(1, 0.5f);
            mAnimation.setDuration(2000);
            mAnimation.setInterpolator(new LinearInterpolator());
            mAnimation.setRepeatCount(Animation.INFINITE);
            mAnimation.setRepeatMode(Animation.REVERSE);
            getView().findViewById(R.id.connectButton).startAnimation(mAnimation);
        } else {
            view.clearAnimation();
        }


    }


    public void disconnectClick(View view) {

        accesstoguide = 0;
        view.clearAnimation();

        getView().findViewById(R.id.connectButton).setBackgroundColor(getResources().getColor(R.color.connect_red));
        //Connect to the controller over FTP and send txt file
        //		if (getAGVActivity().getCon().isConnected()) {
        //			setStatus("DISCONNECT");

        getAGVActivity().disconnect();

        //			//getAGVActivity().connect(ipText, port);
        //
        //		}

        setStatus("DISCONNECT");


    }


    private void sendAccesscode(final String host) {
        if (host != null && NetUtils.isValidIPAddress(host)) {
            final ProgressDialog sendProgress = new ProgressDialog(getAGVActivity());
            sendProgress.setMessage("Sending accesscode");
            sendProgress.setCancelable(false);
            sendProgress.setCanceledOnTouchOutside(false);
            sendProgress.show();
            accesscode = rand.nextInt(9000) + 1000;

            new Thread() {
                @Override
                public void run() {


                    String msg = "";
                    try {
                        SSHClient ssh = new SSHClient();
                        //Add a HostKeyVerifier which will be invoked for verifying host key during
                        // connection establishment and future key exchanges.
                        ssh.addHostKeyVerifier(new PromiscuousVerifier());
                        ssh.connect(host, FTP_PORT);
                        log.debug("Connection made, logging in...");
                        ssh.authPassword(FTP_USER, FTP_PASS);
                        log.debug("Successfully logged in!");
                        SFTPClient sftp = ssh.newSFTPClient();
                        //Session cmd = ssh.startSession();
                        //cmd.exec("chmod 777 " + FTP_REMOTE_DIR + "*");
                        File txt = new File(getTablePath());


                        if (!txt.exists()) {
                            txt.mkdirs();
                        }
                        File accessfile = new File(txt, "accesscode.txt");
                        FileWriter writer = new FileWriter(accessfile);
                        writer.append(accesscode + "");
                        writer.flush();
                        writer.close();


                        //Send the txt file
                        String remote = FTP_REMOTE_DIR + "accesscode.txt";
                        log.debug("Sending txt file to " + remote);
                        sftp.put(accessfile.getPath(), remote);
                        log.debug("txt file stored");


                        //Send the PRM file
                        //remote = FTP_REMOTE_DIR + prm.getName();
                        //log.debug("Sending PRM file to " + remote);
                        //sftp.put(prm.getPath(), remote);
                        //log.debug("PRM file stored");


                        log.debug("Program upload successful, Disconnecting...");

                        //	cmd.close();
                        sftp.close();
                        ssh.disconnect();

                        msg = "accesscode upload successful!";
                    } catch (final IOException e) {
                        msg = " Error sending: " + e.getMessage();
                    } finally {
                        final String fmsg = msg;
                        getAGVActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sendProgress.dismiss();
                                Toast.makeText(getAGVActivity(), fmsg, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }.start();
        } else {
            Toast.makeText(getAGVActivity(), "Invalid IP", Toast.LENGTH_LONG).show();
        }
    }

    private String getTablePath() {
        return String.format("%s/%s/", PROJECTS_DIR, "accesscode");
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
        if (getAGVActivity() != null) {
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
        if (accesstoguide == 1) {
            log.debug("Updating form home fragment");


            con.sendCommand("set speed " + speed.getProgress());
            con.sendCommand("set heading " + heading.getProgress());
            con.sendCommand("get speed");
            con.sendCommand("get heading");
        }
    }

    @Override
    public void processAGVResponse(String message) {
        //							log.debug( data);
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