package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.CurrencyRatesProvider.ConversionRate;
import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Date: 10/28/15
 * Time: 1:15 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrencyRatesJdbcRepository implements CurrencyRatesRepository, JdbcRepository<ConversionRate> {

    public static final String TABLE_NAME = "currency_rate";
    public static final String SEQ_NAME = "seq_currency_rate";
    public static final String INDEX_DAY = "ix_currency_rate_day";
    public static final String INDEX_FROM_TO = "ix_currency_rate_from_to";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_FROM = "from_unit";
    public static final String COL_TO = "to_unit";
    public static final String COL_RATE = "rate";

    private static final Logger logger = LoggerFactory.getLogger(CurrencyRatesJdbcRepository.class);
    private static final ImmutableList<String> COLS = ImmutableList.of(COL_DAY, COL_FROM, COL_TO, COL_RATE);


    private final SafeJdbcConnector jdbcConnector;
    private final RateRowMapper rowMapper = new RateRowMapper();
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    CurrencyRatesJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }


    @Override
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public AgnosticRowMapper<ConversionRate> getRowMapper() {
        return rowMapper;
    }

    @Override
    public SafeJdbcConnector getJdbcConnector() {
        return jdbcConnector;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getIdColumnName() {
        return COL_ID;
    }

    @Override
    public String getSeqName() {
        return SEQ_NAME;
    }

    @Override
    public ImmutableList<String> getColumnNames() {
        return COLS;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return Common.EMPTY_JOINS;
    }

    @Override
    public ImmutableList<?> decomposeObject(ConversionRate object) {
        return ImmutableList.of(object.day, object.pair.from.getNumericCode(), object.pair.to.getNumericCode(), object.rate);
    }

    @Nullable
    @Override
    public Object extractId(ConversionRate object) {
        return null;
    }


    @Override
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        try {
            return Common.insert(this, new ConversionRate(dayUtc, new ConversionPair(from, to), rate)).getKey() != null;
        } catch (RuntimeException ex) {
            logger.warn("addRate() exception", ex);
            return false;
        }
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_RATE);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY, COL_FROM, COL_TO);
        return Common.getSingleColumnOptional(
                this,
                sb.toString(),
                sqlDialect.getRowMapperForType(BigDecimal.class),
                sqlDialect.translateForDb(day), from.getNumericCode(), to.getNumericCode()
        );
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        final StringBuilder sqlBuilder = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_RATE);
        SqlDialect.appendWhereClausePart(sqlBuilder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_FROM, COL_TO);
        SqlDialect.appendWhereClausePostfix(sqlBuilder, sqlDialect, OptLimit.createLimit(1), OrderBy.getDefault(COL_DAY, Order.DESC));
        return Common.getSingleColumnOptional(
                this,
                sqlBuilder.toString(),
                sqlDialect.getRowMapperForType(BigDecimal.class),
                from.getNumericCode(), to.getNumericCode()
        );
    }

    @Override
    public boolean isRateStale(CurrencyUnit to) {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_ID);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY, COL_FROM);
        SqlDialect.appendWhereClausePart(false, sb, false, SqlDialect.Op.EQUAL, COL_TO);
        final int toTrans = to.getNumericCode();
        return !Common.getSingleColumnOptional(
                this,
                sb.toString(),
                sqlDialect.getRowMapperForType(Long.class),
                sqlDialect.translateForDb(new UtcDay()), toTrans, toTrans
        ).isPresent();
    }

    @Override
    public ImmutableSet<Long> getIndexedForDay(UtcDay day) {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_ID);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        return ImmutableSet.copyOf(
                Common.getSingleColumnList(
                        this,
                        sb.toString(),
                        sqlDialect.getRowMapperForType(Long.class),
                        sqlDialect.translateForDb(day)
                )
        );
    }


    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_DAY, TABLE_NAME, false, COL_DAY),
                sqlDialect.createIndexSql(INDEX_FROM_TO, TABLE_NAME, true, COL_DAY, COL_FROM, COL_TO)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.dropIndexCommand(INDEX_DAY),
                SqlDialect.dropIndexCommand(INDEX_FROM_TO),
                SqlDialect.dropTableCommand(TABLE_NAME)
        };
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                + COL_FROM + " INT, "
                + COL_TO + " INT, "
                + COL_RATE + ' ' + sqlDialect.decimalType()
                + ')';
    }


    private final class RateRowMapper implements AgnosticRowMapper<ConversionRate> {

        @Override
        public ConversionRate mapRow(ResultSet rs) throws SQLException {
            final UtcDay day = sqlDialect.translateFromDb(rs.getObject(1), UtcDay.class);
            final int from = rs.getInt(2);
            final int to = rs.getInt(3);
            final BigDecimal rate = sqlDialect.translateFromDb(rs.getObject(4), BigDecimal.class);

            return new ConversionRate(day, new ConversionPair(CurrencyUnit.ofNumericCode(from), CurrencyUnit.ofNumericCode(to)), rate);
        }

    }

}
