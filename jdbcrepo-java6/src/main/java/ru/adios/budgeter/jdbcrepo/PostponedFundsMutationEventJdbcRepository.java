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
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/29/15
 * Time: 4:16 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class PostponedFundsMutationEventJdbcRepository implements PostponedFundsMutationEventRepository, JdbcRepository<PostponedMutationEvent> {

    public static final String TABLE_NAME = "postponed_funds_mutation_event";
    public static final String SEQ_NAME = "seq_postponed_funds_mutation_event";
    public static final String FK_REL_ACC = "fk_fme_relevant_acc";
    public static final String FK_SUBJ = "fk_fme_subject";
    public static final String FK_AGENT = "fk_fme_agent";
    public static final String INDEX_DAY = "ix_fme_day";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_UNIT = "unit";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_RELEVANT_ACCOUNT_ID = "relevant_account_id";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_SUBJECT_ID = "subject_id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_AGENT_ID = "agent_id";
    public static final String COL_CONVERSION_UNIT = "conversion_unit";
    public static final String COL_CUSTOM_RATE = "custom_rate";

    public static final String JOIN_RELEVANT_ACC_ID = "r." + JdbcTreasury.COL_ID;
    public static final String JOIN_RELEVANT_ACC_NAME = "r." + JdbcTreasury.COL_NAME;
    public static final String JOIN_RELEVANT_ACC_CURRENCY_UNIT = "r." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_RELEVANT_ACC_BALANCE = "r." + JdbcTreasury.COL_BALANCE;
    public static final String JOIN_RELEVANT_ACC_DESC = "r." + JdbcTreasury.COL_DESCRIPTION;

    public static final String JOIN_SUBJECT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ID;
    public static final String JOIN_SUBJECT_PARENT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_PARENT_ID;
    public static final String JOIN_SUBJECT_ROOT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ROOT_ID;
    public static final String JOIN_SUBJECT_CHILD_FLAG = "s." + FundsMutationSubjectJdbcRepository.COL_CHILD_FLAG;
    public static final String JOIN_SUBJECT_TYPE = "s." + FundsMutationSubjectJdbcRepository.COL_TYPE;
    public static final String JOIN_SUBJECT_NAME = "s." + FundsMutationSubjectJdbcRepository.COL_NAME;
    public static final String JOIN_SUBJECT_DESC = "s." + FundsMutationSubjectJdbcRepository.COL_DESCRIPTION;

    public static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    public static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;
    public static final String JOIN_AGENT_DESC = "a." + FundsMutationAgentJdbcRepository.COL_DESCRIPTION;

    public static final SqlDialect.Join JOIN_RELEVANT_ACCOUNT =
            SqlDialect.Join.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "r", COL_RELEVANT_ACCOUNT_ID, JdbcTreasury.COL_ID);
    public static final SqlDialect.Join JOIN_SUBJECT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationSubjectJdbcRepository.TABLE_NAME, "s", COL_SUBJECT_ID, FundsMutationSubjectJdbcRepository.COL_ID);
    public static final SqlDialect.Join JOIN_AGENT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID);

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_UNIT, COL_AMOUNT,
            JOIN_RELEVANT_ACC_ID, JOIN_RELEVANT_ACC_NAME, JOIN_RELEVANT_ACC_CURRENCY_UNIT, JOIN_RELEVANT_ACC_BALANCE, JOIN_RELEVANT_ACC_DESC,
            COL_QUANTITY,
            JOIN_SUBJECT_ID, JOIN_SUBJECT_PARENT_ID, JOIN_SUBJECT_ROOT_ID, JOIN_SUBJECT_CHILD_FLAG, JOIN_SUBJECT_TYPE, JOIN_SUBJECT_NAME, JOIN_SUBJECT_DESC,
            COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME, JOIN_AGENT_DESC,
            COL_CONVERSION_UNIT, COL_CUSTOM_RATE
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DAY, COL_UNIT, COL_AMOUNT, COL_RELEVANT_ACCOUNT_ID, COL_QUANTITY, COL_SUBJECT_ID, COL_TIMESTAMP, COL_AGENT_ID, COL_CONVERSION_UNIT, COL_CUSTOM_RATE
    );

    private static final String SQL_STREAM_LOSSES = getStreamInnerSql(SqlDialect.Op.LESS);
    private static final String SQL_STREAM_BENEFITS = getStreamInnerSql(SqlDialect.Op.MORE);
    private static final String SQL_STREAM_EVENTS = getStreamInnerSql(null);
    private static String getStreamInnerSql(@Nullable SqlDialect.Op op) {
        final StringBuilder builder = SqlDialect.Static.selectSqlBuilder(
                TABLE_NAME,
                COLS_FOR_SELECT,
                JOIN_RELEVANT_ACCOUNT,
                JOIN_SUBJECT,
                JOIN_AGENT
        );
        SqlDialect.Static.appendWhereClausePart(true, builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        SqlDialect.Static.appendWhereClausePart(true, builder.append(" AND (("), true, SqlDialect.Op.EQUAL, COL_UNIT, COL_CONVERSION_UNIT);
        SqlDialect.Static.appendWhereClausePart(true, builder.append(") OR ("), true, SqlDialect.Op.EQUAL, COL_UNIT, COL_CONVERSION_UNIT);
        builder.append("))");
        if (op != null) {
            SqlDialect.Static.appendWhereClausePart(false, builder, true, op, COL_AMOUNT);
        }
        return builder.toString();
    }


    private final SafeJdbcConnector jdbcConnector;
    private final JdbcRepository.Default<PostponedMutationEvent> def = new JdbcRepository.Default<PostponedMutationEvent>(this);

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final FundsMutationEventJdbcRepository mutationRepo;
    private final PostponedMutationRowMapper rowMapper = new PostponedMutationRowMapper();
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = def.getInsertSql(false);

    public PostponedFundsMutationEventJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this(jdbcConnector, new FundsMutationEventJdbcRepository(jdbcConnector));
    }

    public PostponedFundsMutationEventJdbcRepository(SafeJdbcConnector jdbcConnector, FundsMutationEventJdbcRepository mutationRepo) {
        this.jdbcConnector = jdbcConnector;
        this.mutationRepo = mutationRepo;
    }


    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<PostponedMutationEvent> getById(Long id) {
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
    public AgnosticRowMapper<PostponedMutationEvent> getRowMapper() {
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
                JOIN_RELEVANT_ACCOUNT,
                JOIN_SUBJECT,
                JOIN_AGENT
        };
    }

    @Override
    public ImmutableList<?> decomposeObject(PostponedMutationEvent object) {
        checkArgument(object.mutationEvent.relevantBalance.id.isPresent(), "Relevant account %s without ID", object.mutationEvent.relevantBalance);
        checkArgument(object.mutationEvent.subject.id.isPresent(), "Subject %s without ID", object.mutationEvent.subject);
        checkArgument(object.mutationEvent.agent.id.isPresent(), "Agent with name %s without ID", object.mutationEvent.agent.name);

        return ImmutableList.of(
                new UtcDay(object.mutationEvent.timestamp),
                object.mutationEvent.amount.getCurrencyUnit().getNumericCode(), object.mutationEvent.amount.getAmount(),
                object.mutationEvent.relevantBalance.id.get(),
                object.mutationEvent.quantity,
                object.mutationEvent.subject.id.getAsLong(),
                object.mutationEvent.timestamp,
                object.mutationEvent.agent.id.getAsLong(),
                object.conversionUnit.getNumericCode(),
                JdbcRepository.Static.wrapNull(object.customRate.orElse(null))
        );
    }

    @Nullable
    @Override
    public Object extractId(PostponedMutationEvent object) {
        return null;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(PostponedMutationEvent object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(PostponedMutationEvent object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public void rememberPostponedExchangeableEvent(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        Common.insert(this, new PostponedMutationEvent(mutationEvent, paidUnit, customRate));
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamInner(day, oneOf, secondOf, SQL_STREAM_BENEFITS, true);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamInner(day, oneOf, secondOf, SQL_STREAM_LOSSES, true);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedEvents(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamInner(day, oneOf, secondOf, SQL_STREAM_EVENTS, false);
    }

    private Stream<PostponedMutationEvent> streamInner(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf, String sql, boolean includeZero) {
        final ImmutableList params = includeZero
                ? ImmutableList.of(day, oneOf.getNumericCode(), secondOf.getNumericCode(), secondOf.getNumericCode(), oneOf.getNumericCode(), 0)
                : ImmutableList.of(day, oneOf.getNumericCode(), secondOf.getNumericCode(), secondOf.getNumericCode(), oneOf.getNumericCode());
        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(jdbcConnector, sqlDialect, sql, params, "streamRememberedExchanges"),
                Common.getMappingSqlFunction(rowMapper, sql, "streamRememberedExchanges")
        );
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                    + COL_UNIT + " INT, "
                    + COL_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_RELEVANT_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_QUANTITY + " INT, "
                    + COL_SUBJECT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + ' ' + sqlDialect.bigIntType() + ", "
                    + COL_CONVERSION_UNIT + " INT, "
                    + COL_CUSTOM_RATE + ' ' + sqlDialect.decimalType() + ", "
                    + sqlDialect.foreignKey(new String[] {COL_RELEVANT_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_REL_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SUBJECT_ID}, FundsMutationSubjectJdbcRepository.TABLE_NAME, new String[] {FundsMutationSubjectJdbcRepository.COL_ID}, FK_SUBJ)
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


    private final class PostponedMutationRowMapper extends AgnosticRowMapper<PostponedMutationEvent> {

        @Override
        public PostponedMutationEvent mapRow(ResultSet rs) throws SQLException {
            final FundsMutationEvent fundsMutationEvent = mutationRepo.getRowMapper().mapRow(rs);
            final int conversionUnit = rs.getInt(20);
            final BigDecimal customRate = sqlDialect.translateFromDb(rs.getObject(21), BigDecimal.class);

            return new PostponedMutationEvent(fundsMutationEvent, CurrencyUnit.ofNumericCode(conversionUnit), Optional.ofNullable(customRate));
        }

    }

}
