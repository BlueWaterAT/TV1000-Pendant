package com.bwat.pendant.agvtable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import com.bwat.pendant.AGVUtils;
import com.bwat.pendant.MathUtils;
import com.bwat.pendant.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Kareem ElFaramawi
 */
public class EditableTable extends TableLayout {
	private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
	private ArrayList<EditableTableColumn> columns = new ArrayList<EditableTableColumn>();
	private EditableTableDataChangeListener changeListener = null;

	private static final float ROW_HEADER_WEIGHT = 0.25f;
	private static final float ROW_CELL_WEIGHT = 1.0f;
	private static final int DIVIDER_OP = LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END;
	private static final int MIN_ROW_HEIGHT = 100;
	private static final char ARROW_UP = '\u25b2';
	private static final char ARROW_DOWN = '\u25bc';

	private int selectedRow = -1;
	private int sortedCol = -1;
	private boolean sortDesc = false;
	private boolean sortEnabled = true;


	public EditableTable(Context context) {
		super(context);
		init(context, null);
	}

	public EditableTable(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}


	private void init(Context context, AttributeSet attrs) {
		//Set up h-divider
		setDividerDrawable(context.getResources().getDrawable(R.drawable.table_divider_h));
		setShowDividers(DIVIDER_OP);

		//For default init
		int rows = 2, cols = 5;
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditableTable);
			for (int i = 0; i < a.getIndexCount(); i++) {
				int attr = a.getIndex(i);
				switch (attr) {
					case R.styleable.EditableTable_rows:
						rows = a.getInt(attr, rows);
						break;
					case R.styleable.EditableTable_cols:
						cols = a.getInt(attr, cols);
						break;
				}
			}
		}
		for (int i = 0; i < cols; i++) {
			addColumn(String.valueOf((char) ('A' + i)), EditableTableCellType.TEXT, false);
		}
		displayHeader();

		for (int i = 0; i < rows; i++) {
			addRow(false);
		}

		updateView(false);
		selectSortCol(0, false);
	}

	public void addColumn(String name, EditableTableCellType type) {
		addColumn(name, type, true);
	}

	public void addColumn(String name, EditableTableCellType type, boolean updateView) {
		EditableTableColumn col = new EditableTableColumn(name, type);
		columns.add(col);
		for (ArrayList<Object> row : data) {
			row.add(col.getDefaultValue());
		}
		if (updateView) {
			updateView(false);
		}
	}

	public void removeColumn(int i) {
		if (!MathUtils.inRange_in_ex(i, 0, getColumnCount())) {
			AGVUtils.logE("Attempted to remove column at invalid index: " + i);
			return;
		}
		columns.remove(i);
		for (ArrayList<Object> row : data) {
			row.remove(i);
		}

		//Update the view
		for (int row = 1; row < getChildCount(); row++) {
			((TableRow) getChildAt(row)).removeViewAt(i + 1);
		}
		selectSortCol(-1);
		updateView(true);
	}

	public void setColumnType(int i, EditableTableCellType type, String... values) {
		if (!MathUtils.inRange_in_ex(i, 0, getColumnCount())) {
			AGVUtils.logE("Attempted to access column at invalid index: " + i);
			return;
		}
		EditableTableColumn col = columns.get(i);
		if (col.getType() != type || !Arrays.equals(col.getValues(), values)) {
			col.setType(type);
			col.setValues(values);
			for (int row = 1; row < getChildCount(); row++) {
				View v = createViewOfType(col);
				setOnChangeListener(v, row - 1, i);
				TableRow tr = (TableRow) getChildAt(row);
				tr.removeViewAt(i + 1);
				tr.addView(v, i + 1);
				data.get(row - 1).set(i, col.getDefaultValue());
			}
		}
	}

	public ArrayList<ArrayList<Object>> getData() {
		return data;
	}

	public void addRow() {
		addRow(true);
	}

	public void addRow(boolean updateView) {
		ArrayList<Object> row = new ArrayList<Object>();
		for (EditableTableColumn col : columns) {
			row.add(col.getDefaultValue());
		}
		data.add(row);
		if (updateView) {
			updateView(false);
		}
	}

	public void removeRow(int i) {
		if (!MathUtils.inRange_in_ex(i, 0, getRowCount())) {
			AGVUtils.logE("Attempted to remove row at invalid index: " + i);
			return;
		}
		data.remove(i);

		//update the view by removing the last row and shifting data up
		removeViewAt(getChildCount() - 1);
		updateViewData();
		selectRow(i);
	}

	public int getRowCount() {
		return data.size();
	}

	/**
	 * @return The number of rows in the view, accounting for the fact that the header is not a row
	 */
	private int getViewRowCount() {
		return getChildCount() - 1;
	}

	public int getColumnCount() {
		return columns.size();
	}

	/**
	 * @return The number of columns in the view, account for the fact the the row numbers is not a column
	 */
	private int getViewColumnCount() {
		return ((TableRow) getChildAt(0)).getChildCount() - 1;
	}

	/**
	 * @param row Row index
	 * @param col Column index
	 * @return The view located at this index, accounting for row and column headers
	 */
	private View getViewAt(int row, int col) {
		return ((TableRow) getChildAt(row + 1)).getChildAt(col + 1);
	}


	public Object getValueAt(int row, int col) {
		if (!MathUtils.inRange_in_ex(row, 0, getRowCount()) || !MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
			AGVUtils.logE(String.format("Attempted to access cell at invalid index: row=%d, col=%d", row, col));
			return null;
		}
		return data.get(row).get(col);
	}

	public void setValueAt(Object val, int row, int col) {
		if (!MathUtils.inRange_in_ex(row, 0, getRowCount()) || !MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
			AGVUtils.logE(String.format("Attempted to access cell at invalid index: row=%d, col=%d", row, col));
			return;
		}
		if (!valueAllowed(val, getColumnType(col))) {
			AGVUtils.logE(String.format("Attempted to set cell to an invalid value: value=%s, colType=%s", val.toString(), getColumnType(col).toString()));
			return;
		}
		data.get(row).set(col, val);
	}

	public EditableTableColumn getColumn(int col) {
		if (!MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
			AGVUtils.logE("Attempted to access column at invalid index: " + col);
			return null;
		}
		return columns.get(col);
	}

	public EditableTableCellType getColumnType(int col) {
		if (!MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
			AGVUtils.logE("Attempted to access column at invalid index: " + col);
			return null;
		}
		return columns.get(col).getType();
	}

	private boolean valueAllowed(Object value, EditableTableCellType type) {
		switch (type) {
			case TEXT:
			case COMBO:
				return value instanceof String;
			case CHECK:
				return value instanceof Boolean;
			case NUMBER:
				return value instanceof Integer;
		}
		return false;
	}

	/**
	 * Updates the TableLayout to reflect changes in the data.
	 * This assumes the view structure is correct and all values are valid types
	 */
	public void updateViewData() {
		int r = getRowCount(), vr = getViewRowCount(), c = getColumnCount(), vc = getViewColumnCount();
		//This really shouldn't ever happen
		if (r != vr || c != vc) {
			AGVUtils.logE("Table model size does not match view, update view structure.");
			return;
		}
		//Copy over data
		for (int row = 0; row < getRowCount(); row++) {
			for (int col = 0; col < getColumnCount(); col++) {
				View v = getViewAt(row, col);
				switch (getColumnType(col)) {
					case TEXT:
						((EditText) v).setText((String) getValueAt(row, col));
						break;
					case COMBO:
						Spinner s = (Spinner) getViewAt(row, col);
						ArrayAdapter<String> adapter = (ArrayAdapter<String>) s.getAdapter();
						s.setSelection(adapter.getPosition((String) getValueAt(row, col)));
						break;
					case CHECK:
						((CheckBox) v).setChecked((Boolean) getValueAt(row, col));
						break;
					case NUMBER:
						((EditText) v).setText(String.valueOf(getValueAt(row, col)));
						break;
				}
			}
		}
	}

	/**
	 * Updates the table layout to reflect the column types and row/column counts. If a column type changes, the data for that column is cleared.
	 */
	public void updateViewStructure(boolean force) {
		if (force) {
			removeAllViews();
			displayHeader();
		}

		//Add missing columns, they should never have to be removed here
		for (int i = getViewColumnCount(); i < getColumnCount(); i++) {
			EditableTableColumn col = columns.get(i);
			for (int row = 1; row < getChildCount(); row++) {
				View v = createViewOfType(col);
				setOnChangeListener(v, row - 1, i);
				((TableRow) getChildAt(row)).addView(v);
			}
		}

		//Add missing rows, they should never have to be removed here
		for (int i = getViewRowCount(); i < getRowCount(); i++) {
			TableRow row = createRow(i + 1); //Number of currently visible rows + 1
			row.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			addView(row);
		}

		//Recreate header in case any columns were removed
		displayHeader();
	}

	private void displayHeader() {
		if (getChildCount() > 0) {
			removeViewAt(0);
		}
		addView(createHeader(), 0);
	}

	private TableRow createHeader() {
		TableRow header = new TableRow(getContext());
		header.setBackgroundColor(Color.LTGRAY);
		header.setDividerDrawable(getResources().getDrawable(R.drawable.table_divider_v));
		header.setShowDividers(DIVIDER_OP);
		Space s = new Space(getContext());
		s.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEADER_WEIGHT));
		header.addView(s);
		for (int i = 0; i < columns.size(); i++) {
			EditableTableColumn col = columns.get(i);
			final TextView text = new TextView(getContext());
			text.setText(col.getName());
			text.setGravity(Gravity.CENTER);
			text.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
			text.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, ROW_CELL_WEIGHT));

			final int finalI = i;//Just for copying into inner classes
			text.setOnClickListener(new OnClickListener() {
				int columnIndex = finalI;

				@Override
				public void onClick(View v) {
					selectSortCol(columnIndex);
				}
			});

			text.setOnLongClickListener(new OnLongClickListener() { //Listener to open a popup menu when you long click a column
				int columnIndex = finalI;

				@Override
				public boolean onLongClick(View v) {
					//Display popup menu for column settings
					PopupMenu pop = new PopupMenu(getContext(), text);
					pop.getMenuInflater().inflate(R.menu.column_popup, pop.getMenu());
					pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
							switch (item.getItemId()) {
								case R.id.colSetType:
									//Create a popup menu with radio buttons to select column type
									alert.setTitle("Select column type");
									final RadioGroup radios = new RadioGroup(getContext());
									for (EditableTableCellType type : EditableTableCellType.values()) {
										RadioButton rb = new RadioButton(getContext());
										rb.setText(type.toString());
										radios.addView(rb);
										if (columns.get(columnIndex).getType() == type) {
											radios.check(rb.getId());
										}
									}
									alert.setView(radios);
									alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											//Depending on the type that was chosen, update the column to reflect that type
											EditableTableCellType type = EditableTableCellType.valueOf(((RadioButton) radios.findViewById(radios.getCheckedRadioButtonId())).getText().toString());
											switch (type) {
												case COMBO:
													//Combo boxes need to have their entries entered before setting the column type
													//This asks for how many entries there will be
													AlertDialog.Builder comboCount = new AlertDialog.Builder(getContext());
													final EditText input = new EditText(getContext());
													input.setInputType(InputType.TYPE_CLASS_NUMBER);
													comboCount.setTitle("How many entries in the combo box?");
													comboCount.setView(input);
													comboCount.setPositiveButton("Next", new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {
															//Now that we know the number of entries, display a list of inputs to get all the entries
															try {
																AlertDialog.Builder comboVals = new AlertDialog.Builder(getContext());
																final int count = Integer.parseInt(input.getText().toString());
																ScrollView sc = new ScrollView(getContext());
																final LinearLayout lin = new LinearLayout(getContext());
																lin.setOrientation(VERTICAL);
																for (int i = 0; i < count; i++) {
																	lin.addView(new EditText(getContext()));
																}
																sc.addView(lin);
																comboVals.setTitle("Enter combo box values");
																comboVals.setView(sc);
																comboVals.setPositiveButton("Set", new DialogInterface.OnClickListener() {
																	@Override
																	public void onClick(DialogInterface dialog, int which) {
																		//All entries are saved now, so set the column type with these entries
																		String[] vals = new String[count];
																		for (int i = 0; i < count; i++) {
																			vals[i] = ((EditText) lin.getChildAt(i)).getText().toString();
																		}
																		setColumnType(columnIndex, EditableTableCellType.COMBO, vals);
																	}
																});
																comboVals.setNegativeButton("Cancel", null);
																comboVals.show();
															} catch (NumberFormatException e) {
																AGVUtils.logE("Invalid number entered in combobox setup");
															}
														}
													});
													comboCount.setNegativeButton("Cancel", null);
													comboCount.show();
													break;
												//Text and combo just set the column with no further info
												case TEXT:
													setColumnType(columnIndex, EditableTableCellType.TEXT);
													break;
												case CHECK:
													setColumnType(columnIndex, EditableTableCellType.CHECK);
													break;
												case NUMBER:
													setColumnType(columnIndex, EditableTableCellType.NUMBER);
													break;
											}
										}
									});
									alert.setNegativeButton("Cancel", null);
									alert.show();
									break;
								case R.id.colRename:
									//Display a popup asking for the new name
									alert.setTitle("Enter Column Name");
									final EditText input = new EditText(getContext());
									alert.setView(input);
									alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											//Set the new name
											columns.get(columnIndex).setName(input.getText().toString());
											updateViewStructure(false);
										}
									});
									alert.setNegativeButton("Cancel", null);
									alert.show();
									break;
								case R.id.colDel:
									//Delete the column
									removeColumn(columnIndex);
									break;
								case R.id.colAdd:
									//Create a popup asking for the new column's name
									alert.setTitle("Enter Column Name");
									final EditText newName = new EditText(getContext());
									alert.setView(newName);
									alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											//Create the new column with the entered name
											addColumn(newName.getText().toString(), EditableTableCellType.TEXT);
										}
									});
									alert.setNegativeButton("Cancel", null);
									alert.show();
									break;
							}
							return true;
						}
					});
					pop.show();
					return true;
				}
			});
			header.addView(text);
			header.setMinimumHeight(MIN_ROW_HEIGHT);
		}
		return header;
	}

	private TableRow createRow(final int rowNum) {
		TableRow row = new TableRow(getContext()) {
			@Override
			public boolean onInterceptTouchEvent(MotionEvent ev) {
				if (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP) {
					performClick();
				}
				return super.onInterceptTouchEvent(ev);
			}
		};

		TextView rowHeader = new TextView(getContext());
		rowHeader.setText(String.valueOf(rowNum));
		rowHeader.setGravity(Gravity.CENTER);
		rowHeader.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, ROW_HEADER_WEIGHT));
		row.addView(rowHeader);

		for (int i = 0; i < columns.size(); i++) {
			View v = createViewOfType(columns.get(i));
			setOnChangeListener(v, rowNum - 1, i);
			row.addView(v);
		}

		row.setClickable(true);
		row.setOnClickListener(new OnClickListener() {
			int rowIndex = rowNum - 1;

			@Override
			public void onClick(View v) {
				selectRow(rowIndex);
			}
		});

		row.setDividerDrawable(getResources().getDrawable(R.drawable.table_divider_v));
		row.setShowDividers(DIVIDER_OP);
		row.setMinimumHeight(MIN_ROW_HEIGHT);

		return row;
	}

	private void setOnChangeListener(final View v, final int row, final int col) {
		switch (columns.get(col).getType()) {
			case TEXT:
				((EditText) v).addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable s) {
						data.get(row).set(col, ((EditText) v).getText().toString());
						invokeChangeListener();
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}
				});
				break;
			case COMBO:
				((Spinner) v).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						data.get(row).set(col, ((Spinner) v).getSelectedItem().toString());
						invokeChangeListener();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						data.get(row).set(col, columns.get(col).getDefaultValue());
						invokeChangeListener();
					}
				});
				break;
			case CHECK:
				((CheckBox) v).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						data.get(row).set(col, ((CheckBox) v).isChecked());
						invokeChangeListener();
					}
				});
				break;
			case NUMBER:
				((EditText) v).addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable s) {
						String raw = ((EditText) v).getText().toString();
						data.get(row).set(col, raw.length() > 0 ? Integer.parseInt(raw) : 0);
						invokeChangeListener();
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}
				});
		}
	}

	@NonNull
	private View createViewOfType(EditableTableColumn col) {
		View v = null;
		switch (col.getType()) {
			case TEXT:
				EditText text = new EditText(getContext());
				text.setGravity(Gravity.CENTER);
				text.setBackgroundResource(0);
				v = text;
				break;
			case COMBO:
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, col.getValues());
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				Spinner s = new Spinner(getContext());
				s.setAdapter(adapter);
				s.setGravity(Gravity.CENTER);
				v = s;
				break;
			case CHECK:
				CheckBox checkBox = new CheckBox(getContext());
				checkBox.setGravity(Gravity.CENTER);
				v = checkBox;
				break;
			case NUMBER:
				EditText num = new EditText(getContext());
				num.setGravity(Gravity.CENTER);
				num.setBackgroundResource(0);
				num.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
				v = num;
				break;
		}
		v.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, ROW_CELL_WEIGHT));
		return v;
	}

	/**
	 * Completely refreshes the table, updating structure and data
	 */
	public void updateView(boolean forceRestructure) {
		updateViewStructure(forceRestructure);
		updateViewData();
	}

	public int getSelectedRow() {
		return selectedRow;
	}

	public void selectRow(int rowIndex) {
		if (!MathUtils.inRange_in_ex(rowIndex, 0, getRowCount())) {
			for (int row = 1; row < getChildCount(); row++) {
				((TableRow) getChildAt(row)).setBackgroundColor(Color.TRANSPARENT);
			}
			selectedRow = -1;
		} else {
			if (selectedRow != -1) {
				((TableRow) getChildAt(selectedRow + 1)).setBackgroundColor(Color.TRANSPARENT);
			}
			selectedRow = rowIndex;
			((TableRow) getChildAt(selectedRow + 1)).setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
		}
	}

	public void selectSortCol(int colIndex, boolean desc) {
		if (!MathUtils.inRange_in_ex(colIndex, 0, getColumnCount())) {
			sortedCol = -1;
			sortDesc = false;
			displayHeader();
		} else {
			if (sortedCol != -1) {
				TextView text = ((TextView) ((TableRow) getChildAt(0)).getChildAt(sortedCol + 1));
				String name = text.getText().toString();
				int i = name.lastIndexOf(" -");
				text.setText(name.substring(0, i != -1 ? i : name.length()));
			}
			sortDesc = desc;
			sortedCol = colIndex;
			((TextView) ((TableRow) getChildAt(0)).getChildAt(sortedCol + 1)).setText(getColumn(sortedCol).getName() + " - " + getSortChar());
			sortColumns();
		}
	}

	public void selectSortCol(int colIndex) {
		selectSortCol(colIndex, sortedCol == colIndex ? !sortDesc : false);
	}

	public void sortColumns() {
		if (sortEnabled && MathUtils.inRange_in_ex(sortedCol, 0, getColumnCount())) {
			//Save the currently selected row to reselect after sorting
			ArrayList<Object> selected = null;
			//Find the column that's currently being edited to keep focus (Only text or number)
			int editCol = -1;
			if (selectedRow != -1) {
				selected = data.get(selectedRow);
				for (int i = 0; i < selected.size(); i++) {
					if (getColumnType(i) == EditableTableCellType.TEXT || getColumnType(i) == EditableTableCellType.NUMBER) {
						if (((TableRow) getChildAt(selectedRow + 1)).getChildAt(i + 1).isFocused()) {
							editCol = i;
							break;
						}
					}
				}
			}
			Collections.sort(data, new Comparator<ArrayList<Object>>() {
				@Override
				public int compare(ArrayList<Object> lhs, ArrayList<Object> rhs) {
					return ((Comparable<Object>) lhs.get(sortedCol)).compareTo(rhs.get(sortedCol));
				}
			});
			if (sortDesc) {
				Collections.reverse(data);
			}
			//temporarily disable sorting while updating the view
			sortEnabled = false;
			updateViewData();
			sortEnabled = true;

			//Find and reselect the previously selected row
			if (selected != null) {
				selectRow(data.indexOf(selected));
				if (editCol != -1) {
					EditText text =	(EditText) ((TableRow) getChildAt(selectedRow + 1)).getChildAt(editCol + 1);
					text.requestFocus();
					text.setSelection(text.getText().length());
				}
			}
		}
	}

	private char getSortChar() {
		return sortDesc ? ARROW_DOWN : ARROW_UP;
	}

	private void invokeChangeListener() {
		if (changeListener != null) {
			changeListener.tableDataChanged(data);
		}
		//Sort the columns every time something changes
		sortColumns();
	}

	public void setOnChangeListener(EditableTableDataChangeListener listener) {
		changeListener = listener;
	}

	public void clearColumns() {
		for (int i = 0, c = getColumnCount(); i < c; i++) {
			removeColumn(0);
		}
	}

	public void clearRows() {
		selectRow(-1);
		for (int i = 0, r = getRowCount(); i < r; i++) {
			removeRow(0);
		}
	}

	public boolean isSortEnabled() {
		return sortEnabled;
	}

	public void setSortEnabled(boolean sortEnabled) {
		this.sortEnabled = sortEnabled;
	}
}
