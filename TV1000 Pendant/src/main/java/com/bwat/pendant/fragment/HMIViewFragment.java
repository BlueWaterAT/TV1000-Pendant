//package com.bwat.pendant.fragment;
//
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.os.Environment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.Toast;
//import com.bwat.pendant.AGVConnection;
//import com.bwat.pendant.util.AGVUtils;
//import com.bwat.pendant.util.MathUtils;
//import com.bwat.pendant.util.NetUtils;
//import com.bwat.pendant.NumberSpinner;
//import com.bwat.pendant.R;
//import com.bwat.pendant.prg.EditableTable;
//import com.bwat.pendant.prg.CellType;
//import com.bwat.pendant.prg.TableDataChangeListener;
//import net.schmizz.sshj.SSHClient;
//import net.schmizz.sshj.sftp.SFTPClient;
//import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Scanner;
//
///**
// * @author Kareem ElFaramawi
// */
//public class HMIViewFragment extends AGVFragment {
//	private EditableTable table;
//	private ArrayList<Object> rowCopy;
//	private ArrayList<ArrayList<Object>> uneditedData;
//	private String openProjectName;
//	private ArrayList<String> tooltips = new ArrayList<String>(); //Useless, but prevent having to change the file format
////	ProgressDialog loading; //Yes it's that slow
//
//	private Button insert;
//	private Button delete;
//	private Button copy;
//	private Button paste;
//	private Button params;
//	private Button saveAs;
//	private Button load;
//	private Button get;
//	private NumberSpinner indexSelector;
//
//
//	private final static String PROJECTS_DIR = Environment.getExternalStorageDirectory().getPath() + "/AGV Programmer";
//	private final static String EXTENSION = ".jtb";
//	private final static String PROGRAM_EXTENSION = ".prg";
//	//public static final String PARAM_EXTENSION = ".prm";
//	private final static String COMMA = ",";
//	private final static String COMMENT = ";";
//
//	//FTP related variables
//	private final static int FTP_PORT = 22;
//	private final static String FTP_USER = "root";
//	private final static String FTP_PASS = "bwat1234";
//	private final static String FTP_REMOTE_DIR = "/hmi/prg/";
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		return inflater.inflate(R.layout.fragment_hmi, container, false);
//	}
//
//	@Override
//	public void onActivityCreated(Bundle savedInstanceState) {
//		super.onActivityCreated(savedInstanceState);
//
//		table = (EditableTable) getView().findViewById(R.id.progView1);
//
//		table.setOnChangeListener(new TableDataChangeListener() {
//			@Override
//			public void tableDataChanged(ArrayList<ArrayList<Object>> data) {
//				if (uneditedData != null && !uneditedData.equals(exportTableData())) {
//					if (openProjectName != null) {
//						saveProgram();
//					}
//				}
//			}
//		});
//
//		insert = ((Button) getView().findViewById(R.id.progInsert1));
//		insert.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				table.addRow();
//			}
//		});
//
//		delete = ((Button) getView().findViewById(R.id.progDel1));
//		delete.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				table.removeRow(table.getSelectedRow());
//			}
//		});
//
//		copy = ((Button) getView().findViewById(R.id.progCopy1));
//		copy.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				int row = table.getSelectedRow();
//				if (MathUtils.inRange_in_ex(row, 0, table.getRowCount())) {
//					rowCopy = new ArrayList<Object>(table.getColumnCount());
//					for (int col = 0; col < table.getColumnCount(); col++) {
//						rowCopy.add(table.getValueAt(row, col));
//					}
//				}
//				paste.setEnabled(true);
//			}
//		});
//
//		paste = ((Button) getView().findViewById(R.id.progPaste1));
//		paste.setEnabled(false);
//		paste.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				int row = table.getSelectedRow();
//				if (MathUtils.inRange_in_ex(row, 0, table.getRowCount()) && rowCopy != null && rowCopy.size() == table.getColumnCount()) {
//					for (int col = 0; col < table.getColumnCount(); col++) {
//						table.setValueAt(rowCopy.get(col), row, col);
//					}
//					table.updateViewData();
//				}
//			}
//		});
//
//		saveAs = ((Button) getView().findViewById(R.id.progSaveAs1));
//		saveAs.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				ArrayList<String> projects = getProjectList();
//
//				AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
//				alert.setTitle("Save As...");
//				alert.setMessage("Select existing project");
//
//				final Spinner select = getProjectListSpinner();
//				alert.setView(select);
//				alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						setOpenProjectName((String) select.getSelectedItem());
//						saveTable();
//					}
//				});
//				alert.setNeutralButton("New...", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
//						alert.setTitle("New Project");
//						alert.setMessage("Enter project name");
//						final EditText input = new EditText(getAGVActivity());
//						alert.setView(input);
//						alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								String proj = input.getText().toString();
//								if (proj.length() > 0) { //Can't have a blank project name
//									setOpenProjectName(proj);
//									try {
//										File out = new File(getTablePath());
//										out.getParentFile().mkdirs();
//										out.createNewFile();
//									} catch (IOException e) {
//										e.printStackTrace();
//									}
//									saveTable();
//								}
//							}
//						});
//						alert.setNegativeButton("Cancel", null);
//						alert.show();
//					}
//				});
//				alert.setNegativeButton("Cancel", null);
//				alert.show();
//			}
//		});
//
//
//		get = ((Button) getView().findViewById(R.id.progSend1));
//		get.setEnabled(true);
//		get.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				//Connect to the controller over FTP and send jtb/prg files
//				if (getAGVActivity().getCon().isConnected()) {
//					getSFTP(getAGVActivity().getCon().getHost());
//
//
//
//
//
//				} else {
//					AlertDialog.Builder alert = new AlertDialog.Builder(getAGVActivity());
//					alert.setTitle("No Controller Connected");
//					alert.setMessage("Manually enter SFTP IP");
//					final EditText input = new EditText(getAGVActivity());
//					alert.setView(input);
//					alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							getSFTP(input.getText().toString());
//
//						}
//					});
//					alert.setNegativeButton("Cancel", null);
//					alert.show();
//
//				}
//
//
//
//
//				}
//
//
//		});
//
//		indexSelector = (NumberSpinner) getView().findViewById(R.id.indexSelector1);
//		indexSelector.setOnChangeListener(new NumberSpinner.ChangeListener() {
//			@Override
//			public void valueChanged() {
//				loadProgram(indexSelector.getValue());
//			}
//		});
//
////		loading = new ProgressDialog(getAGVActivity());
////		loading.setMessage("Loading...");
////		loading.setCancelable(false);
////		loading.setCanceledOnTouchOutside(false);
//
//		if (openProjectName != null) {
//			loadTable();
//		}
//	}
//
//	@Override
//	public void updateAGV(AGVConnection con) {
//
//	}
//
//	@Override
//	public void processAGVResponse(String message) {
//
//	}
//
//	private void getSFTP(final String host) {
//		if (host != null && NetUtils.isValidIPAddress(host)) {
//			final ProgressDialog sendProgress = new ProgressDialog(getAGVActivity());
//			sendProgress.setMessage("Getting");
//			sendProgress.setCancelable(false);
//			sendProgress.setCanceledOnTouchOutside(false);
//			sendProgress.show();
//
//			new Thread() {
//				@Override
//				public void run() {
//					saveProgram(); //Do this before sending anything
//
//					AGVUtils.logD(String.format("Connecting to SFTP server as %s:%s @ %s:%d", FTP_USER, FTP_PASS, host, FTP_PORT));
//					String msg = "";
//					try {
//						SSHClient ssh = new SSHClient();
//						ssh.addHostKeyVerifier(new PromiscuousVerifier());
//						ssh.connect(host, FTP_PORT);
//						AGVUtils.logD("Connection made, logging in...");
//						ssh.authPassword(FTP_USER, FTP_PASS);
//						AGVUtils.logD("Successfully logged in!");
//						SFTPClient sftp = ssh.newSFTPClient();
////						Session cmd = ssh.startSession();
//						//local file
//						String path=PROJECTS_DIR+"/Running/";
//						File run = new File(path);
//
//
//
//						if (!run.exists()) {
//							run.mkdirs();
//						}
//
//						//String remote = FTP_REMOTE_DIR + jtb.getName();
//						String remote = FTP_REMOTE_DIR + "hmi.jtb";
//						AGVUtils.logD("Sending JTB file to " + remote);
//						sftp.get(remote, path + "Running.jtb");
//						AGVUtils.logD("JTB file stored");
//
//						//Get the PRG file
//						//remote = FTP_REMOTE_DIR + prg.getName();
//						remote = FTP_REMOTE_DIR + "hmi-1.prg";
//						AGVUtils.logD("Sending PRG file to " + remote);
//						sftp.get(remote,path + "Running-1.prg" );
//						AGVUtils.logD("PRG file stored");
//
//						//Send the PRM file
//						//remote = FTP_REMOTE_DIR + prm.getName();
//						//remote = FTP_REMOTE_DIR + "hmi.prm";
//						//AGVUtils.logD("Sending PRM file to " + remote);
//						//sftp.get(remote,PROJECTS_DIR + "/Running/hmi-1.prm");
//						//AGVUtils.logD("PRM file stored");
//
////						cmd.exec("chmod 777 " + FTP_REMOTE_DIR + "*");
//						AGVUtils.logD("Program download successful, Disconnecting...");
////						cmd.close();
//						sftp.close();
//						ssh.disconnect();
//						msg = "Program download successful!";
//
//					} catch (final IOException e) {
//						msg = " Error sending: " + e.getMessage();
//					} finally {
//						final String fmsg = msg;
//						getAGVActivity().runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								sendProgress.dismiss();
//								Toast.makeText(getAGVActivity(), fmsg, Toast.LENGTH_LONG).show();
//								setOpenProjectName("Running");
//								loadTable();
//							}
//						});
//					}
//				}
//			}.start();
//		} else {
//			Toast.makeText(getAGVActivity(), "Invalid IP", Toast.LENGTH_LONG).show();
//		}
//	}
//
//	private Object getValueAt(int row, int col) {
//		Object val = table.getValueAt(row, col);
//		return val.equals("") ? null : val;
//	}
//
//	private ArrayList<String> getProjectList() {
//		File projectDir = new File(PROJECTS_DIR);
//		ArrayList<String> projects = new ArrayList<String>();
//		if (projectDir.exists()) {
//			for (File proj : projectDir.listFiles()) {
//				if (proj.isDirectory()) {
//					projects.add(proj.getName());
//				}
//			}
//		}
//		return projects;
//	}
//
//	private Spinner getProjectListSpinner() {
//		ArrayList<String> projects = getProjectList();
//		Spinner select = new Spinner(getAGVActivity());
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getAGVActivity(), android.R.layout.simple_spinner_item, projects);
//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		select.setAdapter(adapter);
//		return select;
//	}
//
//	private String getTablePath() {
//		return String.format("%s/%s/%s%s", PROJECTS_DIR, openProjectName, openProjectName, EXTENSION);
//	}
//
//	private String getProgramPath(int program) {
//		return String.format("%s/%s/%s-%d%s", PROJECTS_DIR, openProjectName, openProjectName, program, PROGRAM_EXTENSION);
//	}
//
//	//private String getParamPath() {
//	//	return String.format("%s/%s/%s%s", PROJECTS_DIR, openProjectName, openProjectName, PARAM_EXTENSION);
//	//}
//
//	public ArrayList<ArrayList<Object>> exportTableData() {
//		ArrayList<ArrayList<Object>> target = new ArrayList<ArrayList<Object>>();
//		for (int row = 0; row < table.getRowCount(); row++) {
//			target.add(new ArrayList<Object>());
//			for (int col = 0; col < table.getColumnCount(); col++) {
//				target.get(row).add(table.getValueAt(row, col));
//			}
//		}
//		return target;
//	}
//
//	public void saveTable() {
//		try {
//			// Save table settings
//			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(getTablePath())));
//			pw.println(COMMENT + "Interactive JTable Save Data");
//			pw.println("\n" + COMMENT + "Column Headers and Tooltips, the number of headers sets the number of columns:");
//			for (int i = 0; i < table.getColumnCount(); i++) {
//				pw.print(table.getColumn(i).getName() + (i == table.getColumnCount() - 1 ? "\n" : COMMA));
//			}
//			for (int i = 0; i < table.getColumnCount(); i++) {
//				pw.print((i < tooltips.size() ? tooltips.get(i) : " ") + (i == table.getColumnCount() - 1 ? "\n" : COMMA));
//			}
//			pw.println("\n" + COMMENT + "The following lines are all the data types of the columns");
//			pw.println(COMMENT + "There are 4 types: Text, Checkbox, Combo Box, and Number. Their syntax is as follows:");
//			pw.printf("%s\"%s\"\n", COMMENT, CellType.TEXT.getTypeName());
//			pw.printf("%s\"%s\"\n", COMMENT, CellType.CHECK.getTypeName());
//			pw.printf("%s\"%s,choice,choice,choice,...\"\n", COMMENT, CellType.COMBO.getTypeName());
//			pw.printf("%s\"%s\"\n", COMMENT, CellType.NUMBER.getTypeName());
//			pw.println(COMMENT + "The number of lines MUST equal the number of columns");
//			for (int i = 0; i < table.getColumnCount(); i++) {
//				switch (table.getColumnType(i)) {
//					case TEXT:
//						pw.println(CellType.TEXT.getTypeName());
//						break;
//					case CHECK:
//						pw.println(CellType.CHECK.getTypeName());
//						break;
//					case COMBO:
//						pw.print(CellType.COMBO.getTypeName() + COMMA);
//						String[] entries = table.getColumn(i).getValues();
//						for (int j = 0; j < entries.length; j++) {
//							pw.print(entries[j] + (j == entries.length - 1 ? "\n" : COMMA));
//						}
//						break;
//					case NUMBER:
//						pw.println(CellType.NUMBER.getTypeName());
//						break;
//				}
//			}
//			pw.flush();
//			pw.close();
//
//			// Save current program
//			saveProgram();
//			params.setEnabled(true);
//			get.setEnabled(true);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void saveProgram() {
//		try {
//			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(getProgramPath(indexSelector.getValue()))));
//			for (int row = 0; row < table.getRowCount(); row++) {
//				for (int col = 0; col < table.getColumnCount(); col++) {
//					pw.print(getValueAt(row, col) + (col == table.getColumnCount() - 1 ? "\n" : COMMA));
//				}
//			}
//			pw.flush();
//			pw.close();
//			uneditedData = exportTableData();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void loadTable() {
//		// Some initial setup
//		params.setEnabled(true);
//		get.setEnabled(true);
//		uneditedData = null;
//		table.setSortEnabled(false);
//		try {
//			Scanner scan = new Scanner(new File(getTablePath()));
//			String[] data;
//			// Get Column Headers
//			data = nextAvailableLine(scan).split(COMMA);
//			final int cols = data.length;
//			String[] colNames = Arrays.copyOf(data, cols);
//			table.clearColumns();
//			//Get tooltips
//			data = nextAvailableLine(scan).split(COMMA);
//			tooltips = new ArrayList<String>(Arrays.asList(data));
//			// Get Column Editor Types
//			for (int i = 0; i < colNames.length; i++) {
//				data = nextAvailableLine(scan).split(COMMA);
//				if (data[0].equals(CellType.TEXT.getTypeName())) {
//					table.addColumn(colNames[i], CellType.TEXT);
//				} else if (data[0].equals(CellType.CHECK.getTypeName())) {
//					table.addColumn(colNames[i], CellType.CHECK);
//				} else if (data[0].equals(CellType.COMBO.getTypeName())) {
//					table.addColumn(colNames[i], CellType.COMBO);
//					table.setColumnType(i, CellType.COMBO, Arrays.copyOfRange(data, 1, data.length));
//				} else if (data[0].equals(CellType.NUMBER.getTypeName())) {
//					table.addColumn(colNames[i], CellType.NUMBER);
//				}
//			}
//			scan.close();
//			indexSelector.setValue(indexSelector.getMin());
//			loadProgram(1);
//			rowCopy = null;
//			paste.setEnabled(false);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		table.setSortEnabled(true);
//		table.selectSortCol(0, false);
//		table.sortColumns();
//	}
//
//	public void loadProgram(int prog) {
//		if (prog > 0) {
//			table.setSortEnabled(false);
//			uneditedData = null;
//			table.clearRows();
//			table.addRow();
//			table.addRow();
//			if (openProjectName != null) {
//				if (new File(getProgramPath(prog)).exists()) {
//					loadTableData(getProgramPath(prog));
//				}
//			}
//			uneditedData = exportTableData();
//			table.setSortEnabled(true);
//			table.selectSortCol(0, false);
//			table.sortColumns();
//		}
//	}
//
//	public void loadTableData(String path) {
//		try {
//			Scanner scan = new Scanner(new File(path));
//			String[] data;
//			String line;
//			table.clearRows();
//			for (int row = 0; scan.hasNext(); row++) {
//				line = scan.nextLine();
//				table.addRow();
//				data = line.split(COMMA);
//				for (int col = 0; col < table.getColumnCount(); col++) {
//					CellType type = table.getColumnType(col);
//					switch (type) {
//						case TEXT:
//						case COMBO:
//							table.setValueAt(data[col].equals("null") ? table.getColumn(col).getDefaultValue() : data[col], row, col);
//							break;
//						case CHECK:
//							String d = data[col].toLowerCase();
//							table.setValueAt(!(d.equals("null") || d.equals("false")), row, col);
//							break;
//						case NUMBER:
//							table.setValueAt(data[col].equals("null") ? (int) table.getColumn(col).getDefaultValue() : Integer.parseInt(data[col]), row, col);
//							break;
//					}
//				}
//			}
//			scan.close();
//			uneditedData = exportTableData();
//			table.updateViewData();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private String nextAvailableLine(Scanner scan) {
//		String line;
//		while ((line = scan.nextLine()).startsWith(COMMENT) || line.length() == 0) {
//		}
//		return line;
//	}
//
//	private void setOpenProjectName(String name) {
//		openProjectName = name;
//		getAGVActivity().getSupportActionBar().setTitle(getString(R.string.fragPathProg) + " - " + name);
//	}
//}
