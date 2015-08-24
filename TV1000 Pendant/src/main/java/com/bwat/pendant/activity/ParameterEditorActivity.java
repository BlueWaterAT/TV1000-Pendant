package com.bwat.pendant.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bwat.pendant.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * @author Kareem ElFaramawi
 */
public class ParameterEditorActivity extends AGVActivity {
	private TableLayout table;
	private int rows = 100;
	private String path;

	private static final float ROW_HEADER_WEIGHT = 0.25f;
	private static final float COL_1_WEIGHT = 1.0f;
	private static final float COL_2_WEIGHT = 7.0f;
	private static final int DIVIDER_OP = LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setTitle(getString(R.string.paramTitle));
		((Toolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		table = (TableLayout) findViewById(R.id.paramTable);
		table.setDividerDrawable(getResources().getDrawable(R.drawable.table_divider_h));
		table.setShowDividers(DIVIDER_OP);

		//Create table header
		TableRow header = new TableRow(this);
		header.setBackgroundColor(Color.LTGRAY);
		header.setDividerDrawable(getResources().getDrawable(R.drawable.table_divider_v));
		header.setShowDividers(DIVIDER_OP);

		Space s = new Space(this);
		s.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, ROW_HEADER_WEIGHT));
		header.addView(s);

		TextView c1 = new TextView(this);
		c1.setText(getString(R.string.paramColData));
		c1.setTextAppearance(this, android.R.style.TextAppearance_Large);
		c1.setGravity(Gravity.CENTER);
		c1.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, COL_1_WEIGHT));
		header.addView(c1);

		TextView c2 = new TextView(this);
		c2.setText(getString(R.string.paramColComm));
		c2.setTextAppearance(this, android.R.style.TextAppearance_Large);
		c2.setGravity(Gravity.CENTER);
		c2.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, COL_2_WEIGHT));
		header.addView(c2);

		table.addView(header);

		rows = getIntent().getIntExtra(getString(R.string.paramKeyRows), rows);
		path = getIntent().getStringExtra(getString(R.string.paramKeyPath));

		for (int i = 0; i < rows; i++) {
			TableRow row = new TableRow(this);
			row.setDividerDrawable(getResources().getDrawable(R.drawable.table_divider_v));
			row.setShowDividers(DIVIDER_OP);

			TextView rowHeader = new TextView(this);
			rowHeader.setText(String.valueOf(i + 1));
			rowHeader.setGravity(Gravity.CENTER);
			rowHeader.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, ROW_HEADER_WEIGHT));
			row.addView(rowHeader);

			EditText in1 = new EditText(this);
			in1.setBackgroundResource(0);
			in1.setGravity(Gravity.CENTER);
			in1.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, COL_1_WEIGHT));
			row.addView(in1);

			EditText in2 = new EditText(this);
			in2.setBackgroundResource(0);
			in2.setGravity(Gravity.CENTER);
			in2.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, COL_2_WEIGHT));
			row.addView(in2);

			table.addView(row);
		}

		if (path != null) {
			loadParameters();
		}

	}

	@Override
	protected int getLayoutResource() {
		return R.layout.activity_param;
	}

	public void close(View v) {
		finish();
	}

	public void saveClose(View v) {
		saveParameters();
		close(v);
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Warning");
		alert.setMessage("Save before closing?");
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveClose(null);
			}
		});
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				close(null);
			}
		});
		alert.show();
	}

	public String getValueAt(int row, int col) {
		return ((EditText) ((TableRow) table.getChildAt(row + 1)).getChildAt(col + 1)).getText().toString();
	}

	public void setValueAt(String val, int row, int col) {
		((EditText) ((TableRow) table.getChildAt(row + 1)).getChildAt(col + 1)).setText(val);
	}

	public void clearAll() {
		for (int row = 0; row < rows; row++) {
			setValueAt("", row, 0);
			setValueAt("", row, 1);
		}
	}

	public void loadParameters() {
		if (path != null && path.length() > 0) {
			File file = new File(path);
			if (file.exists()) {
				clearAll();
				try {
					Scanner scan = new Scanner(file);
					String line;
					while (scan.hasNext()) {
						line = scan.nextLine();
						if (line.length() > 0) {
							String[] csv = line.split(",");
							int row = Integer.parseInt(csv[0]);
							String data = csv[1].equals("null") ? "" : csv[1];
							String comment = csv[2].equals("null") ? "" : csv[2];
							setValueAt(data, row, 0);
							setValueAt(comment, row, 1);
						}
					}
					scan.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void saveParameters() {
		if (path != null && path.length() > 0) {
			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(path)));
				for (int row = 0; row < rows; row++) {
					String data = getValueAt(row, 0);
					String comment = getValueAt(row, 1);

					data = data.length() > 0 ? data : null;
					comment = comment.length() > 0 ? comment : null;
					if (data != null || comment != null) {
						pw.println(row + "," + data + "," + comment);
					}
				}
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
