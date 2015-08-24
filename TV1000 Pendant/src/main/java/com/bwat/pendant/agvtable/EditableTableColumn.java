package com.bwat.pendant.agvtable;

/**
 * @author Kareem ElFaramawi
 */
public class EditableTableColumn {
	private String name;
	private EditableTableCellType type;
	private String[] values;

	public EditableTableColumn(String name, EditableTableCellType type, String... values) {
		this.name = name;
		this.type = type;
		this.values = values;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EditableTableCellType getType() {
		return type;
	}

	public void setType(EditableTableCellType type) {
		this.type = type;
	}

	public Object getDefaultValue() {
		switch (type) {
			case COMBO:
				return values != null && values.length > 0 ? values[0] : type.defVal;
			case TEXT:
			case CHECK:
			default:
				return type.defVal;
		}
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String... values) {
		this.values = values;
	}
}
