package ru.adios.budgeter.jdbcrepo;

import org.intellij.lang.annotations.Language;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 9:24 PM
 *
 * @author Mikhail Kulikov
 */
public final class SqliteDialect implements SqlDialect {

    public static final SqliteDialect INSTANCE = new SqliteDialect();

    private static final String TEXT_TYPE = "TEXT";
    private static final String DECIMAL_TYPE = TEXT_TYPE;
    private static final String PRIMARY_KEY_WITH_NEXT_VALUE = "PRIMARY KEY AUTOINCREMENT";

    @Language("SQLite")
    public static final String SEQUENCE_CURRENT_VALUE_SQL = "SELECT seq FROM sqlite_sequence WHERE name = ";
    public static final String SEQUENCE_SET_VALUE_SQL = "UPDATE sqlite_sequence SET seq = ? WHERE name = ";
    public static final String TABLE_EXISTENCE_QUERY = "SELECT name FROM sqlite_master WHERE type='table' AND name = '";

    private SqliteDialect() {}

    @Override
    public String tableExistsSql(String tableName) {
        return TABLE_EXISTENCE_QUERY + tableName + '\'';
    }

    @Override
    public String textType() {
        return TEXT_TYPE;
    }

    @Override
    public String decimalType() {
        return DECIMAL_TYPE;
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
    public String createSeq(String seqName, String tableName) {
        return null;
    }

    @Override
    public String sequenceCurrentValueSql(@Nullable String tableName, @Nullable String sequenceName) {
        return SEQUENCE_CURRENT_VALUE_SQL + '\'' + tableName + '\'';
    }

    @Override
    public String sequenceSetValueSql(@Nullable String tableName, @Nullable String sequenceName) {
        return SEQUENCE_SET_VALUE_SQL + '\'' + tableName + '\'';
    }

    @Override
    public String insertSql(String tableName, List<String> columns) {
        final int size = columns.size();
        final StringBuilder sb = new StringBuilder(25 + tableName.length() + size * 15);

        sb.append("INSERT INTO ")
                .append(tableName)
                .append(" (");

        SqlDialect.appendColumns(sb, columns);

        sb.append(") VALUES (");

        boolean first = true;
        for (int i = 0; i < size; i++) {
            first = SqlDialect.appendCol(sb, "?", first);
        }
        return sb.append(')').toString();
    }

    @Override
    public String selectAllSql(String tableName) {
        return "SELECT * FROM " + tableName;
    }

    @Override
    public String selectSql(String tableName, @Nullable String whereClause, List<String> columns) {
        final StringBuilder sb = new StringBuilder(20 + tableName.length() + columns.size() * 15);
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

    @Override
    public Object translateForDb(Object object) {
        if (object instanceof BigDecimal) {
            return ((BigDecimal) object).stripTrailingZeros().toPlainString();
        }
        return object;
    }

    @Override
    public <T> T translateFromDb(Object object, Class<T> type) {
        if (BigDecimal.class.equals(type) && object instanceof CharSequence) {
            //noinspection unchecked
            return (T) new BigDecimal(object.toString());
        }
        //noinspection unchecked
        return (T) object;
    }

}
