package ru.adios.budgeter.jdbcrepo;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final int DECIMAL_SCALE = 10;
    private static final String TEXT_TYPE = "TEXT";
    private static final String NUMBER_TYPE = "INTEGER";
    private static final String DECIMAL_TYPE = NUMBER_TYPE;
    private static final String PRIMARY_KEY_WITH_NEXT_VALUE = "PRIMARY KEY AUTOINCREMENT";

    @Language("SQLite")
    public static final String SEQUENCE_CURRENT_VALUE_SQL = "SELECT seq FROM sqlite_sequence WHERE name = ";
    public static final String SEQUENCE_SET_VALUE_SQL = "UPDATE sqlite_sequence SET seq = ? WHERE name = ";
    public static final String TABLE_EXISTENCE_QUERY = "SELECT name FROM sqlite_master WHERE type='table' AND name = '";

    private SqliteDialect() {}

    @Override
    public String checkNameCase(String nameCapitalized) {
        return nameCapitalized.toLowerCase();
    }

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
    public String timestampType() {
        return TEXT_TYPE;
    }

    @Override
    public String timestampWithoutTimezoneType() {
        return NUMBER_TYPE;
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
            BigDecimal dec = ((BigDecimal) object).stripTrailingZeros();
            int scale = dec.scale();

            if (scale >= 0) {
                if (scale > DECIMAL_SCALE) {
                    dec = dec.scaleByPowerOfTen(DECIMAL_SCALE - scale);
                    scale = dec.scale();
                }
                return dec.unscaledValue().longValue() * 10 ^ (DECIMAL_SCALE - scale);
            } else {
                return dec.toBigInteger().longValue() * 10 ^ DECIMAL_SCALE;
            }
        } else if (object instanceof UtcDay) {
            return ((UtcDay) object).inner.toInstant().toEpochMilli();
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T translateFromDb(Object object, Class<T> type) {
        if (BigDecimal.class.equals(type) && object instanceof Number) {
            return (T) BigDecimal.valueOf(((Number) object).longValue(), DECIMAL_SCALE).stripTrailingZeros();
        } else if (UtcDay.class.equals(type) && object instanceof Number) {
            return (T) new UtcDay(((Number) object).longValue());
        }
        return (T) object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SingleColumnRowMapper<T> getRowMapperForType(Class<T> type) {
        if (type.equals(Long.class)) {
            return (SingleColumnRowMapper<T>) Common.LONG_ROW_MAPPER;
        } else if (type.equals(String.class)) {
            return (SingleColumnRowMapper<T>) Common.STRING_ROW_MAPPER;
        } else if (type.equals(BigDecimal.class) || type.equals(UtcDay.class)) {
            return new ArbitrarySingleColumnRowMapper<>(type);
        }
        throw new IllegalArgumentException(type.toString() + " unsupported");
    }

    private final class ArbitrarySingleColumnRowMapper<T> extends SingleColumnRowMapper<T> {

        private ArbitrarySingleColumnRowMapper(Class<T> requiredType) {
            super(requiredType);
        }

        @Override
        protected Object getColumnValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
            return translateFromDb(super.getColumnValue(rs, index, requiredType), requiredType);
        }

    }

}
