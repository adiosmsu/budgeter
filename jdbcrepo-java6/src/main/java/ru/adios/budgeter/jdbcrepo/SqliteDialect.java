package ru.adios.budgeter.jdbcrepo;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import ru.adios.budgeter.DateTimeUtils;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.math.BigInteger;
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
@Immutable
public final class SqliteDialect extends AbstractSqlDialect {

    public static final SqliteDialect INSTANCE = new SqliteDialect();


    private static final int DECIMAL_SCALE = 10;
    private static final String TEXT_TYPE = "TEXT";
    private static final String NUMBER_TYPE = "INTEGER";
    private static final String DECIMAL_TYPE = NUMBER_TYPE;
    private static final String PRIMARY_KEY_WITH_NEXT_VALUE = "PRIMARY KEY AUTOINCREMENT";

    @Language("SQLite")
    private static final String SEQUENCE_CURRENT_VALUE_SQL = "SELECT seq FROM sqlite_sequence WHERE name = ";
    private static final String SEQUENCE_SET_VALUE_SQL = "UPDATE sqlite_sequence SET seq = ? WHERE name = ";
    private static final String TABLE_EXISTENCE_QUERY = "SELECT name FROM sqlite_master WHERE type='table' AND name = '";
    private static final int MAX_LONG_PRECISION = BigInteger.valueOf(Long.MAX_VALUE).toString().length();

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
    public String bigIntType() {
        return NUMBER_TYPE;
    }

    @Override
    public String timestampType() {
        return NUMBER_TYPE;
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
    public String foreignKey(String[] columns, String otherTable, String[] otherColumns, String keyName) {
        final StringBuilder sb = new StringBuilder(70);
        sb.append("FOREIGN KEY(");
        SqlDialect.Static.appendColumns(sb, columns);
        sb.append(") REFERENCES ")
                .append(otherTable)
                .append('(');
        SqlDialect.Static.appendColumns(sb, otherColumns);
        return sb.append(')').toString();
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

        SqlDialect.Static.appendColumns(sb, columns);

        return sb.append(')').toString();
    }

    @Override
    public String createSeq(String seqName, String tableName) {
        return null;
    }

    @Override
    public String dropSeqCommand(String seqName) {
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

        SqlDialect.Static.appendColumns(sb, columns);

        sb.append(") VALUES (");

        boolean first = true;
        for (int i = 0; i < size; i++) {
            first = SqlDialect.Static.appendCol(sb, "?", first);
        }
        return sb.append(')').toString();
    }

    @Override
    public String selectAllSql(String tableName) {
        return "SELECT * FROM " + tableName;
    }

    @Override
    public Object translateForDb(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof BigDecimal) {
            final BigDecimal asDec = (BigDecimal) object;
            final BigDecimal dec = asDec.stripTrailingZeros().scaleByPowerOfTen(DECIMAL_SCALE);
            final long val = dec.longValue();
            if (dec.precision() - dec.scale() > MAX_LONG_PRECISION || (asDec.signum() > 0 && val <= 0)) {
                throw new IllegalArgumentException("Decimal is too large for Sqlite DB: " + asDec);
            }
            return val;
        } else if (object instanceof UtcDay) {
            return ((UtcDay) object).inner.toInstant().toEpochMilli();
        } else if (object instanceof OffsetDateTime) {
            return ((OffsetDateTime) object).toInstant().toEpochMilli();
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T translateFromDb(Object object, Class<T> type) {
        if (object == null) {
            return null;
        }

        if (BigDecimal.class.equals(type) && object instanceof Number) {
            return (T) BigDecimal.valueOf(((Number) object).longValue(), DECIMAL_SCALE).stripTrailingZeros();
        } else if (UtcDay.class.equals(type) && object instanceof Number) {
            return (T) new UtcDay(((Number) object).longValue());
        } else if (OffsetDateTime.class.equals(type) && object instanceof Number) {
            return (T) DateTimeUtils.fromEpochMillis(((Number) object).longValue(), ZoneId.systemDefault());
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
        } else if (type.equals(BigDecimal.class) || type.equals(UtcDay.class) || type.equals(OffsetDateTime.class)) {
            return new ArbitrarySingleColumnRowMapper<T>(type);
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
