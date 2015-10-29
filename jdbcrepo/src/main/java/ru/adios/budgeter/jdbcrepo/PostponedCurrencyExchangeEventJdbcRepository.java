package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository.PostponedExchange;
import ru.adios.budgeter.api.Treasury.BalanceAccount;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/29/15
 * Time: 1:02 AM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventJdbcRepository implements PostponedCurrencyExchangeEventRepository, JdbcRepository<PostponedExchange> {

    public static final String TABLE_NAME = "postponed_currency_exchange_event";
    public static final String SEQ_NAME = "seq_postponed_currency_exchange_event";
    public static final String FK_TO_BUY_ACC = "fk_pce_e_to_buy_acc";
    public static final String FK_SELL_ACC = "fk_pce_e_sell_acc";
    public static final String FK_AGENT = "fk_pce_e_agent";
    public static final String INDEX_DAY = "ix_pce_e_day";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_TO_BUY_AMOUNT = "to_buy_amount";
    public static final String COL_TO_BUY_ACCOUNT_ID = "to_buy_account_id";
    public static final String COL_SELL_ACCOUNT_ID = "sell_account_id";
    public static final String COL_CUSTOM_RATE = "custom_rate";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_AGENT_ID = "agent_id";

    public static final String JOIN_TO_BUY_ACC_ID = "b." + JdbcTreasury.COL_ID;
    public static final String JOIN_TO_BUY_ACC_NAME = "b." + JdbcTreasury.COL_NAME;
    public static final String JOIN_TO_BUY_ACC_CURRENCY_UNIT = "b." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_TO_BUY_ACC_BALANCE = "b." + JdbcTreasury.COL_BALANCE;

    public static final String JOIN_SELL_ACC_ID = "s." + JdbcTreasury.COL_ID;
    public static final String JOIN_SELL_ACC_NAME = "s." + JdbcTreasury.COL_NAME;
    public static final String JOIN_SELL_ACC_CURRENCY_UNIT = "s." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_SELL_ACC_BALANCE = "s." + JdbcTreasury.COL_BALANCE;

    public static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    public static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_TO_BUY_AMOUNT,
            JOIN_TO_BUY_ACC_ID, JOIN_TO_BUY_ACC_NAME, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_TO_BUY_ACC_BALANCE,
            JOIN_SELL_ACC_ID, JOIN_SELL_ACC_NAME, JOIN_SELL_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_BALANCE,
            COL_CUSTOM_RATE, COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DAY, COL_TO_BUY_AMOUNT, COL_TO_BUY_ACCOUNT_ID, COL_SELL_ACCOUNT_ID, COL_CUSTOM_RATE, COL_TIMESTAMP, COL_AGENT_ID
    );


    private final SafeJdbcTemplateProvider jdbcTemplateProvider;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final JdbcTreasury.AccountRowMapper accountRowMapper = new JdbcTreasury.AccountRowMapper(sqlDialect);
    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final PostponedExchangeEventRowMapper rowMapper = new PostponedExchangeEventRowMapper();

    public PostponedCurrencyExchangeEventJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
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
    public AgnosticRowMapper<PostponedExchange> getRowMapper() {
        return rowMapper;
    }

    @Override
    public SafeJdbcTemplateProvider getTemplateProvider() {
        return jdbcTemplateProvider;
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
    public ImmutableList<?> decomposeObject(PostponedExchange object) {
        checkArgument(object.toBuyAccount.id != null, "To-buy account %s without ID", object.toBuyAccount);
        checkArgument(object.sellAccount.id != null, "Sell account %s without ID", object.sellAccount);
        checkArgument(object.agent.id.isPresent(), "Agent with name %s without ID", object.agent.name);

        // every new inserted object is of today
        return ImmutableList.of(
                new UtcDay(), object.toBuy, object.toBuyAccount.id, object.sellAccount.id, object.customRate.orElse(null), object.timestamp, object.agent.id
        );
    }

    @Nullable
    @Override
    public Object extractId(PostponedExchange object) {
        return null;
    }


    @Override
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          BalanceAccount toBuyAccount,
                                          BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent) {
        Common.insert(this, new PostponedExchange(toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent));
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        final StringBuilder builder = SqlDialect.selectSqlBuilder(
                TABLE_NAME,
                COLS_FOR_SELECT,
                SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "b", COL_TO_BUY_ACCOUNT_ID, JdbcTreasury.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "s", COL_SELL_ACCOUNT_ID, JdbcTreasury.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID)
        );
        SqlDialect.appendWhereClausePart(true, builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        SqlDialect.appendWhereClausePart(true, builder.append(" AND (("), true, SqlDialect.Op.EQUAL, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_CURRENCY_UNIT);
        SqlDialect.appendWhereClausePart(true, builder.append(") OR ("), true, SqlDialect.Op.EQUAL, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_CURRENCY_UNIT);
        final String sql = builder.append("))").toString();

        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(jdbcTemplateProvider, sql, ImmutableList.of(day, oneOf, secondOf, secondOf, oneOf), "streamRememberedExchanges"),
                Common.getMappingSqlFunction(rowMapper, sql, "streamRememberedExchanges")
        );
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + " BIGINT " + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                    + COL_TO_BUY_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_TO_BUY_ACCOUNT_ID + " BIGINT, "
                    + COL_SELL_ACCOUNT_ID + " BIGINT, "
                    + COL_CUSTOM_RATE + ' ' + sqlDialect.decimalType() + ", "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + " BIGINT, "
                    + sqlDialect.foreignKey(new String[] {COL_TO_BUY_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_TO_BUY_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SELL_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_SELL_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT)
                + ')';
    }

    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_DAY, TABLE_NAME, false, COL_DAY)
        };
    }


    private final class PostponedExchangeEventRowMapper implements AgnosticRowMapper<PostponedExchange> {

        @Override
        public PostponedExchange mapRow(ResultSet rs) throws SQLException {
            final BigDecimal toBuyAmount = sqlDialect.translateFromDb(rs.getObject(1), BigDecimal.class);
            final BalanceAccount toBuyAccount = accountRowMapper.mapRowStartingFrom(2, rs);
            final BalanceAccount sellAccount = accountRowMapper.mapRowStartingFrom(6, rs);
            final BigDecimal customRate = sqlDialect.translateFromDb(rs.getObject(10), BigDecimal.class);
            final OffsetDateTime timestamp = sqlDialect.translateFromDb(rs.getObject(11), OffsetDateTime.class);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(12, rs);

            return new PostponedExchange(toBuyAmount, toBuyAccount, sellAccount, Optional.ofNullable(customRate), timestamp, agent);
        }

    }

}
