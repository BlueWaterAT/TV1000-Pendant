package com.bwat.pendant.prg;

/**
 * @author Kareem ElFaramawi
 */
public enum CellType {
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

    CellType(Object def) {
        defVal = def;
    }
}
