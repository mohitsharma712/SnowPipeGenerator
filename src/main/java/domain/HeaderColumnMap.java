package domain;

import io.github.millij.poi.ss.model.annotations.Sheet;
import io.github.millij.poi.ss.model.annotations.SheetColumn;

@Sheet
public class HeaderColumnMap {
    @SheetColumn("HEADER_COLUMN")
    String header;
    @SheetColumn("TABLE_COLUMN")
    String column;
    @SheetColumn("DEFAULT_VALUE")
    String defaultVal;
    @SheetColumn("IS_STRING")
    boolean isString;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    public boolean getIsString() {
        return isString;
    }

    public void setIsString(boolean isString) {
        this.isString = isString;
    }
}
