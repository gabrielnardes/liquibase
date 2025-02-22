package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.SetColumnRemarksStatement;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;
import liquibase.util.StringUtil;

public class SetColumnRemarksGenerator extends AbstractSqlGenerator<SetColumnRemarksStatement> {
    @Override
    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    @Override
    public boolean supports(SetColumnRemarksStatement statement, Database database) {
        return (database instanceof OracleDatabase) || (database instanceof PostgresDatabase) || (database instanceof
            AbstractDb2Database) || (database instanceof MSSQLDatabase) || (database instanceof H2Database) || (database
            instanceof SybaseASADatabase) || (database instanceof MySQLDatabase);
    }

    @Override
    public ValidationErrors validate(SetColumnRemarksStatement setColumnRemarksStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", setColumnRemarksStatement.getTableName());
        validationErrors.checkRequiredField("columnName", setColumnRemarksStatement.getColumnName());
        validationErrors.checkDisallowedField("catalogName", setColumnRemarksStatement.getCatalogName(), database, MSSQLDatabase.class);
        if (database instanceof MySQLDatabase) {
            validationErrors.checkRequiredField("columnDataType", StringUtil.trimToNull(setColumnRemarksStatement.getColumnDataType()));
        }
        return validationErrors;
    }

    @Override
    public Warnings warn(SetColumnRemarksStatement statementType, Database database, SqlGeneratorChain<SetColumnRemarksStatement> sqlGeneratorChain) {
        final Warnings warnings = super.warn(statementType, database, sqlGeneratorChain);
        if (database instanceof MySQLDatabase) {
            ((MySQLDatabase) database).warnAboutAlterColumn("setColumnRemarks", warnings);
        }

        return warnings;
    }

    @Override
    public Sql[] generateSql(SetColumnRemarksStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {

        String remarksEscaped = database.escapeStringForDatabase(StringUtil.trimToEmpty(statement.getRemarks()));

        if (database instanceof MySQLDatabase) {
            // generate mysql sql  ALTER TABLE cat.user MODIFY COLUMN id int DEFAULT 1001  COMMENT 'A String'
            return new Sql[]{new UnparsedSql("ALTER TABLE " + database.escapeTableName(
                    statement.getCatalogName(), statement.getSchemaName(), statement.getTableName()) + " MODIFY COLUMN "
                    + database.escapeColumnName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), statement.getColumnName()) + " "
                    + DataTypeFactory.getInstance().fromDescription(statement.getColumnDataType(), database).toDatabaseDataType(database)
                    + " COMMENT '" + remarksEscaped + "'", getAffectedColumn(statement))};
        } else if (database instanceof MSSQLDatabase) {
            String schemaName = statement.getSchemaName();
            if (schemaName == null) {
                schemaName = database.getDefaultSchemaName();
            }
            if (schemaName == null) {
                schemaName = "dbo";
            }

            Sql[] generatedSql = {new UnparsedSql("DECLARE @TableName SYSNAME " +
                    "set @TableName = N'" +statement.getTableName() + "'; " +
                    "DECLARE @FullTableName SYSNAME " +
                    "set @FullTableName = N'" + schemaName + "." + statement.getTableName() + "'; " +
                    "DECLARE @ColumnName SYSNAME " +
                    "set @ColumnName = N'" + statement.getColumnName() + "'; " +
                    "DECLARE @MS_DescriptionValue NVARCHAR(3749); " +
                    "SET @MS_DescriptionValue = N'" + remarksEscaped + "';" +
                    "DECLARE @MS_Description NVARCHAR(3749) " +
                    "set @MS_Description = NULL; " +
                    "SET @MS_Description = (SELECT CAST(Value AS NVARCHAR(3749)) AS [MS_Description] " +
                    "FROM sys.extended_properties AS ep " +
                    "WHERE ep.major_id = OBJECT_ID(@FullTableName) " +
                    "AND ep.minor_id=COLUMNPROPERTY(ep.major_id, @ColumnName, 'ColumnId') " +
                    "AND ep.name = N'MS_Description'); " +
                    "IF @MS_Description IS NULL " +
                    "BEGIN " +
                    "EXEC sys.sp_addextendedproperty " +
                    "@name  = N'MS_Description', " +
                    "@value = @MS_DescriptionValue, " +
                    "@level0type = N'SCHEMA', " +
                    "@level0name = N'" + schemaName + "', " +
                    "@level1type = N'TABLE', " +
                    "@level1name = @TableName, " +
                    "@level2type = N'COLUMN', " +
                    "@level2name = @ColumnName; " +
                    "END " +
                    "ELSE " +
                    "BEGIN " +
                    "EXEC sys.sp_updateextendedproperty " +
                    "@name  = N'MS_Description', " +
                    "@value = @MS_DescriptionValue, " +
                    "@level0type = N'SCHEMA', " +
                    "@level0name = N'" + schemaName + "', " +
                    "@level1type = N'TABLE', " +
                    "@level1name = @TableName, " +
                    "@level2type = N'COLUMN', " +
                    "@level2name = @ColumnName; " +
                    "END")};

            return generatedSql;
        } else {
            return new Sql[]{new UnparsedSql("COMMENT ON COLUMN " + database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName())
                    + "." + database.escapeColumnName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), statement.getColumnName()) + " IS '"
                    + remarksEscaped + "'", getAffectedColumn(statement))};
        }
    }

    protected Column getAffectedColumn(SetColumnRemarksStatement statement) {
        return new Column().setName(statement.getColumnName()).setRelation(new Table().setName(statement.getTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName()));
    }
}
