package com.bwat.pendant.prg;

/**
 * @author Kareem ElFaramawi
 */
public class TableColumn {
    private String name;
    private CellType type;
    private String[] values;

    public TableColumn(String name, CellType type, String... values) {
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

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
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
