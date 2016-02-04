package com.bwat.pendant.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.bwat.pendant.AGVConnection;
import com.bwat.pendant.R;
import com.bwat.pendant.prg.PagedProgramTable;
import com.bwat.pendant.prg.ProgramTable;
import com.bwat.pendant.prg.TableDataChangeListener;
import com.bwat.pendant.util.MathUtils;
import com.bwat.pendant.util.NetUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.bwat.pendant.Constants.*;

/**
 * @author Kareem ElFaramawi
 */
public class PathProgrammingFragment extends AGVFragment {
    Logger log = LoggerFactory.getLogger(getClass());

    private PagedProgramTable paged;
    private ProgramTable table;

    // Holds a copy of a row's data
    private ArrayList<Object> rowCopy = new ArrayList<Object>();
    //    private ArrayList<ArrayList<Object>> uneditedData;
    private String openProjectName;

    private Button insert;
    private Button delete;
    private Button copy;
    private Button paste;
    private Button saveAs;
    private Button load;
    private Button send;
    private Button download;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_path_programming, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        paged = (PagedProgramTable) getView().findViewById(R.id.pagedProgView);
        table = (ProgramTable) getView().findViewById(R.id.progView);
        insert = ((Button) getView().findViewById(R.id.progInsert));
        delete = ((Button) getView().findViewById(R.id.progDel));
        copy = ((Button) getView().findViewById(R.id.progCopy));
        paste = ((Button) getView().findViewById(R.id.progPaste));
        saveAs = ((Button) getView().findViewById(R.id.progSaveAs));
        load = ((Button) getView().findViewById(R.id.progLoad));
        send = ((Button) getView().findViewById(R.id.progSend));
        download = ((Button) getView().findViewById(R.id.progDownload));

        // Initial button states
        paste.setEnabled(false); // Can be enabled once a row is copied

        // Need a table loaded to be enabled
        insert.setEnabled(false);
        delete.setEnabled(false);
        copy.setEnabled(false);
        send.setEnabled(false);
        download.setEnabled(false);

        table.setOnChangeListener(new TableDataChangeListener() {
            @Override
            public void tableDataChanged(ArrayList<ArrayList<Object>> data) {
                //                if (uneditedData != null && !uneditedData.equals(exportTableData())) {
                if (!paged.isSelfChanging() && openProjectName != null) {
                    //                        table.saveTableToPath(getTablePath());
                    //                        paged.savePage();
                }
                //                }
            }
        });

        insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paged.insertRow();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paged.deleteRow(table.getSelectedRow());
            }
        });

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyRow(table.getSelectedRow());
            }
        });

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteRow(table.getSelectedRow());
            }
        });

        saveAs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> projects = getProjectList();

                AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
                alert.setTitle("Save As...");
                alert.setMessage("Select existing project");

                final Spinner select = getProjectListSpinner();
                alert.setView(select);
                alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setOpenProjectName((String) select.getSelectedItem());
                        saveTableAs();
                    }
                });
                alert.setNeutralButton("New...", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
                        alert.setTitle("New Project");
                        alert.setMessage("Enter project name");
                        final EditText input = new EditText(getAGVActivity());
                        alert.setView(input);
                        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String proj = input.getText().toString();
                                if (proj.length() > 0) { //Can't have a blank project name
                                    setOpenProjectName(proj);
                                    try {
                                        File out = new File(getTablePath());
                                        out.getParentFile().mkdirs();
                                        out.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    saveTableAs();
                                }
                            }
                        });
                        alert.setNegativeButton("Cancel", null);
                        alert.show();
                    }
                });
                alert.setNegativeButton("Cancel", null);
                alert.show();
            }
        });

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> projects = getProjectList();

                //Display the selection menu
                AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
                alert.setTitle("Load Table");
                if (projects.isEmpty()) {
                    TextView none = new TextView(getAGVActivity());
                    alert.setMessage("No projects available");
                    alert.setNegativeButton("OK", null);
                } else {
                    final Spinner select = getProjectListSpinner();
                    alert.setView(select);
                    alert.setMessage("Select a project");
                    alert.setPositiveButton("Load", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setOpenProjectName((String) select.getSelectedItem());
                            loadTable();
                        }
                    });
                    alert.setNegativeButton("Cancel", null);
                }
                alert.show();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Connect to the controller over FTP and send jtb/prg files
                if (getAGVActivity().getCon().isConnected()) {
                    sendSFTP(getAGVActivity().getCon().getHost());
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
                    alert.setTitle("No Controller Connected");
                    alert.setMessage("Manually enter SFTP IP");
                    final EditText input = new EditText(getAGVActivity());
                    alert.setView(input);
                    alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendSFTP(input.getText().toString());
                        }
                    });
                    alert.setNegativeButton("Cancel", null);
                    alert.show();
                }
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //		loading = new ProgressDialog(getAGVActivity());
        //		loading.setMessage("Loading...");
        //		loading.setCancelable(false);
        //		loading.setCanceledOnTouchOutside(false);

        if (openProjectName != null) {
            loadTable();
        }
    }

    @Override
    public void updateAGV(AGVConnection con) {

    }

    @Override
    public void processAGVResponse(String message) {

    }

    /**
     * Copies the data from a row into a list
     *
     * @param row Table row index
     */
    public void copyRow(int row) {
        // Range validation
        if (MathUtils.inRange_in_ex(row, 0, table.getRowCount())) {
            paste.setEnabled(true);
            rowCopy.clear();
            // Copy the table data
            for (int col = 0; col < table.getColumnCount(); col++) {
                rowCopy.add(table.getValueAt(row, col));
            }
        } else {
            log.error("Invalid row index to copy from: {}", row);
        }
    }

    /**
     * Pastes the data saved in a list into a row
     *
     * @param row Table row index
     */
    public void pasteRow(int row) {
        // Validation
        if (MathUtils.inRange_in_ex(row, 0, table.getRowCount()) && rowCopy.size() == table.getColumnCount()) {
            // Copy over all the data
            for (int col = 0; col < table.getColumnCount(); col++) {
                table.setValueAt(rowCopy.get(col), row, col);
            }
        }
    }

    private void sendSFTP(final String host) {
        if (host != null && NetUtils.isValidIPAddress(host)) {
            final ProgressDialog sendProgress = new ProgressDialog(getAGVActivity());
            sendProgress.setMessage("Sending");
            sendProgress.setCancelable(false);
            sendProgress.setCanceledOnTouchOutside(false);
            sendProgress.show();

            // Save the current page
            paged.savePage();

            new Thread() {
                @Override
                public void run() {

                    log.debug(String.format("Connecting to SFTP server as %s:%s @ %s:%d", FTP_USER, FTP_PASS, host, FTP_PORT));
                    String msg = "";
                    try {
                        SSHClient ssh = new SSHClient();
                        ssh.addHostKeyVerifier(new PromiscuousVerifier());
                        ssh.connect(host, FTP_PORT);
                        log.debug("Connection made, logging in...");
                        ssh.authPassword(FTP_USER, FTP_PASS);
                        log.debug("Successfully logged in!");
                        SFTPClient sftp = ssh.newSFTPClient();
                        //						Session cmd = ssh.startSession();

                        File jtb = new File(getTablePath());
                        File prg = new File(getProgramPath(PROGRAM_DEFAULT));
                        //prm = new File(getParamPath());
                        if (!jtb.exists()) {
                            jtb.mkdirs();
                        }

                        //Send the JTB file
                        String remote = FTP_REMOTE_DIR + "hmi.jtb";
                        log.debug("Sending JTB file to " + remote);
                        sftp.put(jtb.getPath(), remote);
                        log.debug("JTB file stored");

                        //Send the PRG file
                        remote = FTP_REMOTE_DIR + "hmi-1.prg";
                        log.debug("Sending PRG file to " + remote);
                        sftp.put(prg.getPath(), remote);
                        log.debug("PRG file stored");

                        //Send the PRM file
                        //remote = FTP_REMOTE_DIR + prm.getName();
                        //log.debug("Sending PRM file to " + remote);
                        //sftp.put(prm.getPath(), remote);
                        //log.debug("PRM file stored");

                        //						cmd.exec("chmod 777 " + FTP_REMOTE_DIR + "*");
                        log.debug("Program upload successful, Disconnecting...");

                        //						cmd.close();
                        sftp.close();
                        ssh.disconnect();

                        msg = "Program upload successful!";
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

    private Object getValueAt(int row, int col) {
        Object val = table.getValueAt(row, col);
        return val.equals("") ? null : val;
    }

    private ArrayList<String> getProjectList() {
        File projectDir = new File(PROJECTS_DIR);
        ArrayList<String> projects = new ArrayList<String>();
        if (projectDir.exists()) {
            for (File proj : projectDir.listFiles()) {
                if (proj.isDirectory()) {
                    projects.add(proj.getName());
                }
            }
        }
        return projects;
    }

    private Spinner getProjectListSpinner() {
        ArrayList<String> projects = getProjectList();
        Spinner select = new Spinner(getAGVActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getAGVActivity(), android.R.layout.simple_spinner_item, projects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        select.setAdapter(adapter);
        return select;
    }

    //.jtb
    private String getTablePath() {
        return String.format("%s/%s/%s%s", PROJECTS_DIR, openProjectName, openProjectName, EXTENSION);
    }

    //.prg
    private String getProgramPath(int program) {
        return String.format("%s/%s/%s-%d%s", PROJECTS_DIR, openProjectName, openProjectName, program, PROGRAM_EXTENSION);
    }

    public ArrayList<ArrayList<Object>> exportTableData() {
        ArrayList<ArrayList<Object>> target = new ArrayList<ArrayList<Object>>();
        for (int row = 0; row < table.getRowCount(); row++) {
            target.add(new ArrayList<Object>());
            for (int col = 0; col < table.getColumnCount(); col++) {
                target.get(row).add(table.getValueAt(row, col));
            }
        }
        return target;
    }

    public void saveTableAs() {
        // Some setup
        insert.setEnabled(true);
        delete.setEnabled(true);
        copy.setEnabled(true);
        send.setEnabled(true);
        download.setEnabled(true);
        paste.setEnabled(false);

        // Save the JTB
        table.saveTableToPath(getTablePath());
        paged.loadProgram(getProgramPath(PROGRAM_DEFAULT));
    }

    //    public void saveProgram() {
    //        try {
    //            PrintWriter pw = new PrintWriter(new FileOutputStream(new File(getProgramPath(PROGRAM_DEFAULT))));
    //            for (int row = 0; row < table.getRowCount(); row++) {
    //                for (int col = 0; col < table.getColumnCount(); col++) {
    //                    pw.print(getValueAt(row, col) + (col == table.getColumnCount() - 1 ? "\n" : COMMA));
    //                }
    //            }
    //            pw.flush();
    //            pw.close();
    ////            uneditedData = exportTableData();
    //        } catch (FileNotFoundException e) {
    //            e.printStackTrace();
    //        }
    //    }

    public void loadTable() {
        // Some initial setup
        insert.setEnabled(true);
        delete.setEnabled(true);
        copy.setEnabled(true);
        send.setEnabled(true);
        download.setEnabled(true);
        paste.setEnabled(false);
        rowCopy.clear();

        // Load the JTB and PRG
        paged.loadTableFromFile(getTablePath());
        paged.loadProgram(getProgramPath(PROGRAM_DEFAULT));
    }

    private void setOpenProjectName(String name) {
        openProjectName = name;
        getAGVActivity().getSupportActionBar().setTitle(getString(R.string.fragPathProg) + " - " + name);
    }
}
