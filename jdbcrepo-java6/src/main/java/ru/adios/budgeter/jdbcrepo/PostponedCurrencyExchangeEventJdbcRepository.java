/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.OptionalLong;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.PostponedExchange;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/29/15
 * Time: 1:02 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class PostponedCurrencyExchangeEventJdbcRepository implements PostponedCurrencyExchangeEventRepository, JdbcRepository<PostponedExchange> {

    private static final Logger logger = LoggerFactory.getLogger(PostponedCurrencyExchangeEventJdbcRepository.class);

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
    public static final String COL_RELEVANT = "relevant";

    public static final String JOIN_TO_BUY_ACC_ID = "b." + JdbcTreasury.COL_ID;
    public static final String JOIN_TO_BUY_ACC_NAME = "b." + JdbcTreasury.COL_NAME;
    public static final String JOIN_TO_BUY_ACC_CURRENCY_UNIT = "b." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_TO_BUY_ACC_BALANCE = "b." + JdbcTreasury.COL_BALANCE;
    public static final String JOIN_TO_BUY_ACC_DESC = "b." + JdbcTreasury.COL_DESCRIPTION;

    public static final String JOIN_SELL_ACC_ID = "s." + JdbcTreasury.COL_ID;
    public static final String JOIN_SELL_ACC_NAME = "s." + JdbcTreasury.COL_NAME;
    public static final String JOIN_SELL_ACC_CURRENCY_UNIT = "s." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_SELL_ACC_BALANCE = "s." + JdbcTreasury.COL_BALANCE;
    public static final String JOIN_SELL_ACC_DESC = "s." + JdbcTreasury.COL_DESCRIPTION;

    public static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    public static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;
    public static final String JOIN_AGENT_DESC = "a." + FundsMutationAgentJdbcRepository.COL_DESCRIPTION;

    public static final SqlDialect.Join JOIN_TO_BUY_ACCOUNT =
            SqlDialect.Join.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "b", COL_TO_BUY_ACCOUNT_ID, JdbcTreasury.COL_ID);
    public static final SqlDialect.Join JOIN_SELL_ACCOUNT =
            SqlDialect.Join.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "s", COL_SELL_ACCOUNT_ID, JdbcTreasury.COL_ID);
    public static final SqlDialect.Join JOIN_AGENT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID);

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            TABLE_NAME + '.' + COL_ID,
            COL_TO_BUY_AMOUNT,
            JOIN_TO_BUY_ACC_ID, JOIN_TO_BUY_ACC_NAME, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_TO_BUY_ACC_BALANCE, JOIN_TO_BUY_ACC_DESC,
            JOIN_SELL_ACC_ID, JOIN_SELL_ACC_NAME, JOIN_SELL_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_BALANCE, JOIN_SELL_ACC_DESC,
            COL_CUSTOM_RATE, COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME, JOIN_AGENT_DESC,
            COL_RELEVANT
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DAY, COL_TO_BUY_AMOUNT, COL_TO_BUY_ACCOUNT_ID, COL_SELL_ACCOUNT_ID, COL_CUSTOM_RATE, COL_TIMESTAMP, COL_AGENT_ID, COL_RELEVANT
    );

    private static final String SQL_STREAM_REM_EX = getExStreamSql();
    private static String getExStreamSql() {
        final StringBuilder builder = SqlDialect.Static.selectSqlBuilder(
                TABLE_NAME,
                COLS_FOR_SELECT,
                JOIN_TO_BUY_ACCOUNT,
                JOIN_SELL_ACCOUNT,
                JOIN_AGENT
        );
        SqlDialect.Static.appendWhereClausePart(true, builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        SqlDialect.Static.appendWhereClausePart(true, builder.append(" AND "), true, SqlDialect.Op.EQUAL, COL_RELEVANT);
        SqlDialect.Static.appendWhereClausePart(true, builder.append(" AND (("), true, SqlDialect.Op.EQUAL, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_CURRENCY_UNIT);
        SqlDialect.Static.appendWhereClausePart(true, builder.append(") OR ("), true, SqlDialect.Op.EQUAL, JOIN_TO_BUY_ACC_CURRENCY_UNIT, JOIN_SELL_ACC_CURRENCY_UNIT);
        return builder.append("))").toString();
    }
    private static final String SQL_UPDATE_RELEVANCE = SqlDialect.Static.getUpdateSqlStandard(TABLE_NAME, ImmutableList.of(COL_RELEVANT), ImmutableList.of(COL_ID));


    private final SafeJdbcConnector jdbcConnector;
    private final JdbcRepository.Default<PostponedExchange> def = new JdbcRepository.Default<PostponedExchange>(this);

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final JdbcTreasury.AccountRowMapper accountRowMapper = new JdbcTreasury.AccountRowMapper(sqlDialect);
    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final PostponedExchangeEventRowMapper rowMapper = new PostponedExchangeEventRowMapper();
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = def.getInsertSql(false);

    public PostponedCurrencyExchangeEventJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }


    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<PostponedExchange> getById(Long id) {
        return def.getById(id);
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
                JOIN_TO_BUY_ACCOUNT,
                JOIN_SELL_ACCOUNT,
                JOIN_AGENT
        };
    }

    @Override
    public ImmutableList<?> decomposeObject(PostponedExchange object) {
        checkArgument(object.toBuyAccount.id.isPresent(), "To-buy account %s without ID", object.toBuyAccount);
        checkArgument(object.sellAccount.id.isPresent(), "Sell account %s without ID", object.sellAccount);
        checkArgument(object.agent.id.isPresent(), "Agent with name %s without ID", object.agent.name);

        return ImmutableList.of(
                new UtcDay(object.timestamp),
                object.toBuy,
                object.toBuyAccount.id.get(),
                object.sellAccount.id.get(),
                JdbcRepository.Static.wrapNull(object.customRate.orElse(null)),
                object.timestamp,
                object.agent.id.getAsLong(),
                object.relevant
        );
    }

    @Nullable
    @Override
    public Object extractId(PostponedExchange object) {
        return null;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(PostponedExchange object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(PostponedExchange object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          BalanceAccount toBuyAccount,
                                          BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent) {
        Common.insert(this, new PostponedExchange(OptionalLong.empty(), toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent, true));
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(
                        jdbcConnector, sqlDialect, SQL_STREAM_REM_EX,
                        ImmutableList.of(day, true, oneOf.getNumericCode(), secondOf.getNumericCode(), secondOf.getNumericCode(), oneOf.getNumericCode()),
                        "streamRememberedExchanges"
                ),
                Common.getMappingSqlFunction(rowMapper, SQL_STREAM_REM_EX, "streamRememberedExchanges")
        );
    }

    @Override
    public boolean markEventProcessed(PostponedExchange exchange) {
        checkArgument(exchange.id.isPresent());
        try {
            jdbcConnector.getJdbcTemplate().update(SQL_UPDATE_RELEVANCE, false, exchange.id.getAsLong());
            return true;
        } catch (RuntimeException ex) {
            logger.error("Postponed task relevance update failed", ex);
            return false;
        }
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                    + COL_TO_BUY_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_TO_BUY_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_SELL_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_CUSTOM_RATE + ' ' + sqlDialect.decimalType() + ", "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_RELEVANT + " BOOLEAN, "
                    + sqlDialect.foreignKey(new String[] {COL_TO_BUY_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_TO_BUY_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SELL_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_SELL_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT)
                + ')';
    }

    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_DAY, TABLE_NAME, false, COL_DAY)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.Static.dropIndexCommand(INDEX_DAY),
                SqlDialect.Static.dropTableCommand(TABLE_NAME)
        };
    }

    @Override
    public void bootstrap(Logger logger) {}


    private final class PostponedExchangeEventRowMapper extends AgnosticRowMapper<PostponedExchange> {

        @Override
        public PostponedExchange mapRow(ResultSet rs) throws SQLException {
            final long id = rs.getLong(1);
            final BigDecimal toBuyAmount = sqlDialect.translateFromDb(rs.getObject(2), BigDecimal.class);
            final BalanceAccount toBuyAccount = accountRowMapper.mapRowStartingFrom(3, rs);
            final BalanceAccount sellAccount = accountRowMapper.mapRowStartingFrom(8, rs);
            final BigDecimal customRate = sqlDialect.translateFromDb(rs.getObject(13), BigDecimal.class);
            final OffsetDateTime timestamp = sqlDialect.translateFromDb(rs.getObject(14), OffsetDateTime.class);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(15, rs);
            final boolean relevant = rs.getBoolean(18);

            return new PostponedExchange(OptionalLong.of(id), toBuyAmount, toBuyAccount, sellAccount, Optional.ofNullable(customRate), timestamp, agent, relevant);
        }

    }

}
