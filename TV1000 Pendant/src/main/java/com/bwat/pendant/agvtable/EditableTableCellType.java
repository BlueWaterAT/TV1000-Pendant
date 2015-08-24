package com.bwat.pendant.agvtable;

/**
 * @author Kareem ElFaramawi
 */
public enum EditableTableCellType {
	TEXT(""),
	COMBO(""),
	CHECK(false),
	NUMBER(0);

	/**
	 * @return Type name for use with the file format
	 */
	public String getTypeName() {
		return name().toLowerCase();
	}

	public Object defVal;

	EditableTableCellType(Object def) {
		defVal = def;
	}
}
