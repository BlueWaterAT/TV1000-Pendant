package com.bwat.pendant.prg;

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
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.bwat.pendant.R;
import com.bwat.pendant.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static com.bwat.pendant.Constants.*;

/**
 * @author Kareem ElFaramawi
 */
public class ProgramTable extends TableLayout {
    Logger log = LoggerFactory.getLogger(getClass());

    private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
    private ArrayList<TableColumn> columns = new ArrayList<TableColumn>();
    private TableDataChangeListener changeListener = null;

    private ArrayList<String> tooltips = new ArrayList<String>(); //Useless, but prevent having to change the file format

    private static final float ROW_HEADER_WEIGHT = 0.25f;
    private static final float ROW_CELL_WEIGHT = 1.0f;
    private static final int DIVIDER_OP = LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END;
    private static final int MIN_ROW_HEIGHT = 100;

    private int selectedRow = -1;


    public ProgramTable(Context context) {
        super(context);
        init(context, null);
    }

    public ProgramTable(Context context, AttributeSet attrs) {
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
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgramTable);
            for (int i = 0; i < a.getIndexCount(); i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    case R.styleable.ProgramTable_rows:
                        rows = a.getInt(attr, rows);
                        break;
                    case R.styleable.ProgramTable_cols:
                        cols = a.getInt(attr, cols);
                        break;
                }
            }
        }
        for (int i = 0; i < cols; i++) {
            addColumn(String.valueOf((char) ('A' + i)), CellType.TEXT, false);
        }
        displayHeader();

        for (int i = 0; i < rows; i++) {
            addRow(false);
        }

        updateView(false);
    }

    public void addColumn(String name, CellType type) {
        addColumn(name, type, true);
    }

    public void addColumn(String name, CellType type, boolean updateView) {
        TableColumn col = new TableColumn(name, type);
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
            log.error("Attempted to remove column at invalid index: " + i);
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
        updateView(true);
    }

    public void setColumnType(int i, CellType type, String... values) {
        if (!MathUtils.inRange_in_ex(i, 0, getColumnCount())) {
            log.error("Attempted to access column at invalid index: " + i);
            return;
        }
        TableColumn col = columns.get(i);
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
        for (TableColumn col : columns) {
            row.add(col.getDefaultValue());
        }
        data.add(row);
        if (updateView) {
            updateView(false);
        }
    }

    public void removeRow(int i) {
        if (!MathUtils.inRange_in_ex(i, 0, getRowCount())) {
            log.error("Attempted to remove row at invalid index: " + i);
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
            log.error(String.format("Attempted to access cell at invalid index: row=%d, col=%d", row, col));
            return null;
        }
        return data.get(row).get(col);
    }

    public void setValueAt(Object val, int row, int col) {
        if (!MathUtils.inRange_in_ex(row, 0, getRowCount()) || !MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
            log.error(String.format("Attempted to access cell at invalid index: row=%d, col=%d", row, col));
            return;
        }

        if (val == null) {
            val = getColumnType(col).defVal;
        }

        if (!valueAllowed(val, getColumnType(col))) {
            log.error(String.format("Attempted to set cell to an invalid value: value=%s, colType=%s", String.valueOf(val), getColumnType(col).toString()));
            return;
        }
        data.get(row).set(col, val);
    }

    public TableColumn getColumn(int col) {
        if (!MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
            log.error("Attempted to access column at invalid index: " + col);
            return null;
        }
        return columns.get(col);
    }

    public CellType getColumnType(int col) {
        if (!MathUtils.inRange_in_ex(col, 0, getColumnCount())) {
            log.error("Attempted to access column at invalid index: " + col);
            return null;
        }
        return columns.get(col).getType();
    }

    private boolean valueAllowed(Object value, CellType type) {
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
            log.error("Table model size does not match view, update view structure.");
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
            TableColumn col = columns.get(i);
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
            TableColumn col = columns.get(i);
            final TextView text = new TextView(getContext());
            text.setText(col.getName());
            text.setGravity(Gravity.CENTER);
            text.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
            text.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, ROW_CELL_WEIGHT));

            final int finalI = i;//Just for copying into inner classes
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
                                    for (CellType type : CellType.values()) {
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
                                            CellType type = CellType.valueOf(((RadioButton) radios.findViewById(radios.getCheckedRadioButtonId())).getText().toString());
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
                                                                        setColumnType(columnIndex, CellType.COMBO, vals);
                                                                    }
                                                                });
                                                                comboVals.setNegativeButton("Cancel", null);
                                                                comboVals.show();
                                                            } catch (NumberFormatException e) {
                                                                log.error("Invalid number entered in combobox setup");
                                                            }
                                                        }
                                                    });
                                                    comboCount.setNegativeButton("Cancel", null);
                                                    comboCount.show();
                                                    break;
                                                //Text and combo just set the column with no further info
                                                case TEXT:
                                                    setColumnType(columnIndex, CellType.TEXT);
                                                    break;
                                                case CHECK:
                                                    setColumnType(columnIndex, CellType.CHECK);
                                                    break;
                                                case NUMBER:
                                                    setColumnType(columnIndex, CellType.NUMBER);
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
                                            addColumn(newName.getText().toString(), CellType.TEXT);
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
                        invokeChangeListener(); //TODO: I hope this doesnt get weird with autosaving
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
    private View createViewOfType(TableColumn col) {
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

    private void invokeChangeListener() {
        if (changeListener != null) {
            changeListener.tableDataChanged(data);
        }
    }

    public void setOnChangeListener(TableDataChangeListener listener) {
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

    /**
     * Loads the JTB data and formats the table
     *
     * @param path Path to the JTB file
     */
    public void loadTableFromFile(String path) {
        try {
            Scanner scan = new Scanner(new File(path)); // Open file stream
            String[] data; // Holds temp data
            // Read and set the column headers
            data = nextAvailableLine(scan).split(COMMA);
            final int cols = data.length;
            String[] colNames = Arrays.copyOf(data, cols);
            clearColumns();

            // Read and set the column tooltips
            data = nextAvailableLine(scan).split(COMMA);
            tooltips = new ArrayList<String>(Arrays.asList(data));

            // Read and set all the column types
            // Get Column Editor Types
            for (int i = 0; i < colNames.length; i++) {
                data = nextAvailableLine(scan).split(COMMA);
                if (data[0].equals(CellType.TEXT.getTypeName())) {
                    addColumn(colNames[i], CellType.TEXT);
                } else if (data[0].equals(CellType.CHECK.getTypeName())) {
                    addColumn(colNames[i], CellType.CHECK);
                } else if (data[0].equals(CellType.COMBO.getTypeName())) {
                    addColumn(colNames[i], CellType.COMBO);
                    setColumnType(i, CellType.COMBO, Arrays.copyOfRange(data, 1, data.length));
                } else if (data[0].equals(CellType.NUMBER.getTypeName())) {
                    addColumn(colNames[i], CellType.NUMBER);
                }
            }
            scan.close();
            log.info("JTB file \"{}\" successfully loaded", path);
            updateViewStructure(true);
        } catch (FileNotFoundException e) {
            log.info("JTB file \"{}\" not found", path);
            e.printStackTrace();
        }
    }

    /**
     * Reads the file until a line that is not a comment and not blank is found
     *
     * @param scan File Scanner
     * @return The next line that has any content
     */
    private String nextAvailableLine(Scanner scan) {
        String line;
        // Keep reading until a line is found
        while ((line = scan.nextLine()).startsWith(COMMENT) || line.length() == 0) ;
        return line;
    }

    /**
     * Saves the JTB table and creates a blank PRG file
     *
     * @param path Path to save the JTB
     */
    public void saveTableToPath(String path) {
        // Extension fix
        if (!path.endsWith(EXTENSION)) {
            path += EXTENSION;
        }

        try {
            // Save table settings
            PrintWriter pw = new PrintWriter(new FileOutputStream(new File(path)));
            pw.println(COMMENT + "Interactive JTable Save Data");
            pw.println("\n" + COMMENT + "Column Headers and Tooltips, the number of headers sets the number of columns:");

            // Print out all the column headers and tooltips
            for (int i = 0; i < getColumnCount(); i++) {
                pw.print(getColumn(i).getName() + (i == getColumnCount() - 1 ? "\n" : COMMA));
            }
            for (int i = 0; i < getColumnCount(); i++) {
                pw.print(tooltips.get(i) + (i == getColumnCount() - 1 ? "\n" : COMMA));
            }

            pw.println("\n" + COMMENT + "The following lines are all the data types of the columns");
            pw.println(COMMENT + "There are 4 types: Text, Checkbox, Combo Box, and Number. Their syntax is as follows:");
            pw.printf("%s\"%s\"\n", COMMENT, CellType.TEXT.getTypeName());
            pw.printf("%s\"%s\"\n", COMMENT, CellType.CHECK.getTypeName());
            pw.printf("%s\"%s,choice,choice,choice,...\"\n", COMMENT, CellType.COMBO.getTypeName());
            pw.printf("%s\"%s\"\n", COMMENT, CellType.NUMBER.getTypeName());
            pw.println(COMMENT + "The number of lines MUST equal the number of columns");

            // Print out all of the column types
            for (int i = 0; i < getColumnCount(); i++) {
                switch (getColumnType(i)) {
                    case TEXT:
                        pw.println(CellType.TEXT.getTypeName());
                        break;
                    case CHECK:
                        pw.println(CellType.CHECK.getTypeName());
                        break;
                    case COMBO:
                        pw.print(CellType.COMBO.getTypeName() + COMMA);
                        String[] entries = getColumn(i).getValues();
                        for (int j = 0; j < entries.length; j++) {
                            pw.print(entries[j] + (j == entries.length - 1 ? "\n" : COMMA));
                        }
                        break;
                    case NUMBER:
                        pw.println(CellType.NUMBER.getTypeName());
                        break;
                }
            }
            pw.flush();
            pw.close();
            log.info("JTB file \"{}\" successfully saved", path);

            // Create a blank PRG file if it doesn't exist
            int index = PROGRAM_DEFAULT;
            if (index > 0) {
                path = path.substring(0, path.lastIndexOf(EXTENSION)) + "-" + index + PROGRAM_EXTENSION;
                if (!(new File(path).exists())) {
                    new PrintWriter(new FileOutputStream(new File(path))).close();
                    log.info("Blank PRG file successfully saved to \"{}\"", path);
                }
            }
        } catch (FileNotFoundException e) {
            log.error("Error creating JTB file");
            e.printStackTrace();
        }
    }
}
