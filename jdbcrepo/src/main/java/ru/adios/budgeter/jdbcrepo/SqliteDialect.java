package ru.adios.budgeter.jdbcrepo;

import org.intellij.lang.annotations.Language;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 9:24 PM
 *
 * @author Mikhail Kulikov
 */
public final class SqliteDialect implements SqlDialect {

    public static final SqliteDialect INSTANCE = new SqliteDialect();

    private static final String INTEGER_TYPE = "INTEGER";
    private static final String TEXT_TYPE = "TEXT";
    private static final String PRIMARY_KEY_WITH_NEXT_VALUE = "PRIMARY KEY AUTOINCREMENT";

    @Language("SQLite")
    public static final String SEQUENCE_CURRENT_VALUE_SQL = "SELECT seq FROM sqlite_sequence WHERE name = ?";
    public static final String TABLE_EXISTENCE_QUERY = "SELECT name FROM sqlite_master WHERE type='table' AND name = '";

    private SqliteDialect() {}

    @Override
    public String tableExistsSql(String tableName) {
        return TABLE_EXISTENCE_QUERY + tableName + '\'';
    }

    @Override
    public String integerType() {
        return INTEGER_TYPE;
    }

    @Override
    public String textType() {
        return TEXT_TYPE;
    }

    @Override
    public String primaryKeyWithNextValue(@Nullable String sequence) {
        return PRIMARY_KEY_WITH_NEXT_VALUE;
    }

    @Override
    public String createIndexSql(String indexName, String tableName, boolean unique, String... columns) {
        checkArgument(columns.length > 0, "no columns");

        final StringBuilder sb = new StringBuilder(25 + tableName.length() + columns.length * 15);
        sb.append("CREATE ");
        if (unique) {
            sb.append("UNIQUE ");
        }
        sb.append("INDEX ")
                .append(indexName)
                .append(" ON ")
                .append(tableName)
                .append(" (");

        SqlDialect.appendColumns(sb, columns);

        return sb.append(')').toString();
    }

    @Override
    public String sequenceCurrentValueSql(@Nullable String tableName, @Nullable String sequenceName) {
        return SEQUENCE_CURRENT_VALUE_SQL;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public String insertSql(String tableName, String... columns) {
        final StringBuilder sb = new StringBuilder(25 + tableName.length() + columns.length * 15);
        sb.append("INSERT INTO ")
                .append(tableName)
                .append(" (");
        final int columnsLength = columns.length;
        boolean first = true;
        for (int i = 0; i < columnsLength; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        first = true;
        for (int i = 0; i < columnsLength; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.append(')').toString();
    }

    @Override
    public String selectAllSql(String tableName) {
        return "SELECT * FROM " + tableName;
    }

    @Override
    public String selectSql(String tableName, @Nullable String whereClause, String... columns) {
        final StringBuilder sb = new StringBuilder(20 + tableName.length() + columns.length * 15);
        sb.append("SELECT ");
        if (SqlDialect.appendColumns(sb, columns)) {
            sb.append('*');
        }
        sb.append(" FROM ").append(tableName).append(" WHERE ");
        if (whereClause != null) {
            sb.append(whereClause);
        }
        return sb.toString();
    }

}
