package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 8:20 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrencyExchangeEventJdbcRepository implements CurrencyExchangeEventRepository, JdbcRepository<CurrencyExchangeEvent> {

    public static final String TABLE_NAME = "currency_exchange_event";
    public static final String SEQ_NAME = "seq_currency_exchange_event";
    public static final String FK_SOLD_ACC = "fk_cee_sold_acc";
    public static final String FK_BOUGHT_ACC = "fk_cee_bought_acc";
    public static final String FK_AGENT = "fk_cee_agent";
    public static final String COL_ID = "id";
    public static final String COL_SOLD_UNIT = "sold_unit";
    public static final String COL_SOLD_AMOUNT = "sold_amount";
    public static final String COL_BOUGHT_UNIT = "bought_unit";
    public static final String COL_BOUGHT_AMOUNT = "bought_amount";
    public static final String COL_SOLD_ACCOUNT_ID = "sold_account_id";
    public static final String COL_BOUGHT_ACCOUNT_ID = "bought_account_id";
    public static final String COL_RATE = "rate";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_AGENT_ID = "agent_id";

    private static final String JOIN_SOLD_ACC_ID = "s." + JdbcTreasury.COL_ID;
    private static final String JOIN_SOLD_ACC_NAME = "s." + JdbcTreasury.COL_NAME;
    private static final String JOIN_SOLD_ACC_CURRENCY_UNIT = "s." + JdbcTreasury.COL_CURRENCY_UNIT;
    private static final String JOIN_SOLD_ACC_BALANCE = "s." + JdbcTreasury.COL_BALANCE;

    private static final String JOIN_BOUGHT_ACC_ID = "b." + JdbcTreasury.COL_ID;
    private static final String JOIN_BOUGHT_ACC_NAME = "b." + JdbcTreasury.COL_NAME;
    private static final String JOIN_BOUGHT_ACC_CURRENCY_UNIT = "b." + JdbcTreasury.COL_CURRENCY_UNIT;
    private static final String JOIN_BOUGHT_ACC_BALANCE = "b." + JdbcTreasury.COL_BALANCE;

    private static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    private static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;

    private static final SqlDialect.Join JOIN_SOLD_ACCOUNT =
            SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "s", COL_SOLD_ACCOUNT_ID, JdbcTreasury.COL_ID);
    private static final SqlDialect.Join JOIN_BOUGHT_ACCOUNT =
            SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "b", COL_BOUGHT_ACCOUNT_ID, JdbcTreasury.COL_ID);
    private static final SqlDialect.Join JOIN_AGENT =
            SqlDialect.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID);

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_SOLD_UNIT, COL_SOLD_AMOUNT, COL_BOUGHT_UNIT, COL_BOUGHT_AMOUNT,
            JOIN_SOLD_ACC_ID, JOIN_SOLD_ACC_NAME, JOIN_SOLD_ACC_CURRENCY_UNIT, JOIN_SOLD_ACC_BALANCE,
            JOIN_BOUGHT_ACC_ID, JOIN_BOUGHT_ACC_NAME, JOIN_BOUGHT_ACC_CURRENCY_UNIT, JOIN_BOUGHT_ACC_BALANCE,
            COL_RATE, COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_SOLD_UNIT, COL_SOLD_AMOUNT, COL_BOUGHT_UNIT, COL_BOUGHT_AMOUNT, COL_SOLD_ACCOUNT_ID, COL_BOUGHT_ACCOUNT_ID, COL_RATE, COL_TIMESTAMP, COL_AGENT_ID
    );

    private static final String SQL_STREAM_START = SqlDialect.selectSqlBuilder(
            TABLE_NAME, COLS_FOR_SELECT,
            JOIN_SOLD_ACCOUNT,
            JOIN_BOUGHT_ACCOUNT,
            JOIN_AGENT
    ).toString();
    private static final String SQL_STREAM_FOR_DAY = getStreamForDaySql();
    private static String getStreamForDaySql() {
        final StringBuilder builder = new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START);
        SqlDialect.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.MORE_EQ, COL_TIMESTAMP);
        SqlDialect.appendWhereClausePart(false, builder, true, SqlDialect.Op.LESS, COL_TIMESTAMP);
        return builder.toString();
    }

    private final SafeJdbcConnector jdbcConnector;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final JdbcTreasury.AccountRowMapper accountRowMapper = new JdbcTreasury.AccountRowMapper(sqlDialect);
    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final ExchangeEventRowMapper rowMapper = new ExchangeEventRowMapper();
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = JdbcRepository.super.getInsertSql(false);

    CurrencyExchangeEventJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }


    @Override
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public ImmutableList<?> decomposeObject(CurrencyExchangeEvent object) {
        checkArgument(object.soldAccount.id != null, "Sold account %s without ID", object.soldAccount);
        checkArgument(object.boughtAccount.id != null, "Bought account %s without ID", object.boughtAccount);
        checkArgument(object.agent.id.isPresent(), "Agent with name %s without ID", object.agent.name);

        return ImmutableList.of(
                object.sold.getCurrencyUnit().getNumericCode(), object.sold.getAmount(),
                object.bought.getCurrencyUnit().getNumericCode(), object.bought.getAmount(),
                object.soldAccount.id, object.boughtAccount.id,
                object.rate,
                object.timestamp,
                object.agent.id.getAsLong()
        );
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public AgnosticRowMapper<CurrencyExchangeEvent> getRowMapper() {
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
        return COLS_FOR_SELECT;
    }

    @Override
    public ImmutableList<String> getColumnNamesForInsert(boolean withId) {
        return COLS_FOR_INSERT;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return new SqlDialect.Join[] {
                JOIN_SOLD_ACCOUNT,
                JOIN_BOUGHT_ACCOUNT,
                JOIN_AGENT
        };
    }

    @Nullable
    @Override
    public Object extractId(CurrencyExchangeEvent object) {
        return null;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        Common.insert(this, exchangeEvent);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        final StringBuilder sb = new StringBuilder(SQL_STREAM_START.length() + 20 * options.size() + 15).append(SQL_STREAM_START);
        SqlDialect.appendWhereClausePostfix(sb, sqlDialect, limit, Common.translateOrderBy(options));
        final String sql = sb.toString();

        return LazyResultSetIterator.stream(
                Common.getRsSupplier(jdbcConnector, sql, "streamExchangeEvents"),
                Common.getMappingSqlFunction(rowMapper, sql, "streamExchangeEvents")
        );
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return Common.streamForDay(this, SQL_STREAM_FOR_DAY, day, "streamForDay");
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_SOLD_UNIT + " INT, "
                    + COL_SOLD_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_BOUGHT_UNIT + " INT, "
                    + COL_BOUGHT_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_SOLD_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_BOUGHT_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_RATE + ' ' + sqlDialect.decimalType() + ", "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SOLD_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_SOLD_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_BOUGHT_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_BOUGHT_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT)
                + ')';
    }

    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.dropTableCommand(TABLE_NAME)
        };
    }


    private final class ExchangeEventRowMapper implements AgnosticRowMapper<CurrencyExchangeEvent> {

        @Override
        public CurrencyExchangeEvent mapRow(ResultSet rs) throws SQLException {
            final int soldUnit = rs.getInt(1);
            final BigDecimal soldAmount = sqlDialect.translateFromDb(rs.getObject(2), BigDecimal.class);
            final int boughtUnit = rs.getInt(3);
            final BigDecimal boughtAmount = sqlDialect.translateFromDb(rs.getObject(4), BigDecimal.class);
            final Treasury.BalanceAccount soldAccount = accountRowMapper.mapRowStartingFrom(5, rs);
            final Treasury.BalanceAccount boughtAccount = accountRowMapper.mapRowStartingFrom(9, rs);
            final BigDecimal rate = sqlDialect.translateFromDb(rs.getObject(13), BigDecimal.class);
            final OffsetDateTime timestamp = sqlDialect.translateFromDb(rs.getObject(14), OffsetDateTime.class);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(15, rs);

            return CurrencyExchangeEvent.builder()
                    .setSold(Money.of(CurrencyUnit.ofNumericCode(soldUnit), soldAmount))
                    .setBought(Money.of(CurrencyUnit.ofNumericCode(boughtUnit), boughtAmount))
                    .setSoldAccount(soldAccount)
                    .setBoughtAccount(boughtAccount)
                    .setRate(rate)
                    .setTimestamp(timestamp)
                    .setAgent(agent)
                    .build();
        }

    }

}
