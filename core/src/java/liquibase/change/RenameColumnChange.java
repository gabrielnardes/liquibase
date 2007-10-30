package liquibase.change;

import liquibase.database.*;
import liquibase.database.sql.RawSqlStatement;
import liquibase.database.sql.SqlStatement;
import liquibase.database.structure.Column;
import liquibase.database.structure.DatabaseObject;
import liquibase.database.structure.Table;
import liquibase.exception.UnsupportedChangeException;
import liquibase.util.SqlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Renames an existing column.
 */
public class RenameColumnChange extends AbstractChange {

    private String schemaName;
    private String tableName;
    private String oldColumnName;
    private String newColumnName;
    private String columnDataType;

    public RenameColumnChange() {
        super("renameColumn", "Rename Column");
    }


    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getOldColumnName() {
        return oldColumnName;
    }

    public void setOldColumnName(String oldColumnName) {
        this.oldColumnName = oldColumnName;
    }

    public String getNewColumnName() {
        return newColumnName;
    }

    public void setNewColumnName(String newColumnName) {
        this.newColumnName = newColumnName;
    }

    public String getColumnDataType() {
        return columnDataType;
    }

    public void setColumnDataType(String columnDataType) {
        this.columnDataType = columnDataType;
    }

    public SqlStatement[] generateStatements(Database database) throws UnsupportedChangeException {
        if (database instanceof MSSQLDatabase) {
            return new SqlStatement[]{new RawSqlStatement("exec sp_rename '" + database.escapeTableName(getSchemaName(), tableName) + "." + oldColumnName + "', '" + newColumnName+"'")};
        } else if (database instanceof MySQLDatabase) {
            if (columnDataType == null) {
                throw new RuntimeException("columnDataType is required to rename columns with MySQL");
            }
            
            return new SqlStatement[]{new RawSqlStatement("ALTER TABLE " + database.escapeTableName(getSchemaName(), tableName) + " CHANGE " + oldColumnName + " " + newColumnName + " " + columnDataType)};
        } else if (database instanceof DerbyDatabase) {
            throw new UnsupportedChangeException("Derby does not currently support renaming columns");
        } else if (database instanceof HsqlDatabase) {
            return new SqlStatement[]{new RawSqlStatement("ALTER TABLE " + database.escapeTableName(getSchemaName(), tableName) + " ALTER COLUMN " + oldColumnName + " RENAME TO " + newColumnName)};
        } else if (database instanceof FirebirdDatabase) {
            return new SqlStatement[]{new RawSqlStatement("ALTER TABLE " + database.escapeTableName(getSchemaName(), tableName) + " ALTER COLUMN " + oldColumnName + " TO " + newColumnName)};
        } else if (database instanceof DB2Database) {
            throw new UnsupportedChangeException("Rename Column not supported in DB2");
        } else if (database instanceof CacheDatabase) {
            throw new UnsupportedChangeException("Rename Column not currently supported for Cache");
        }

        return new SqlStatement[]{new RawSqlStatement("ALTER TABLE " + database.escapeTableName(getSchemaName(), tableName) + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName)};
    }

    protected Change[] createInverses() {
        RenameColumnChange inverse = new RenameColumnChange();
        inverse.setSchemaName(getSchemaName());
        inverse.setTableName(getTableName());
        inverse.setOldColumnName(getNewColumnName());
        inverse.setNewColumnName(getOldColumnName());
        inverse.setColumnDataType(getColumnDataType());

        return new Change[]{
                inverse
        };
    }

    public String getConfirmationMessage() {
        return "Column "+tableName+"."+ oldColumnName + " renamed to " + newColumnName;
    }

    public Element createNode(Document currentChangeLogFileDOM) {
        Element node = currentChangeLogFileDOM.createElement("renameColumn");
        if (getSchemaName() != null) {
            node.setAttribute("schemaName", getSchemaName());
        }
        
        node.setAttribute("tableName", getTableName());
        node.setAttribute("oldColumnName", getOldColumnName());
        node.setAttribute("newColumnName", getNewColumnName());

        return node;
    }

    public Set<DatabaseObject> getAffectedDatabaseObjects() {

        Table table = new Table(getTableName());

        Column oldColumn = new Column();
        oldColumn.setTable(table);
        oldColumn.setName(getOldColumnName());

        Column newColumn = new Column();
        newColumn.setTable(table);
        newColumn.setName(getNewColumnName());

        return new HashSet<DatabaseObject>(Arrays.asList(table, oldColumn, newColumn));

    }

}
