package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/28/15
 * Time: 6:10 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventJdbcRepository implements FundsMutationEventRepository, JdbcRepository<FundsMutationEvent> {

    public static final String TABLE_NAME = "funds_mutation_event";
    public static final String SEQ_NAME = "seq_funds_mutation_event";
    public static final String FK_REL_ACC = "fk_fme_relevant_acc";
    public static final String FK_SUBJ = "fk_fme_subject";
    public static final String FK_AGENT = "fk_fme_agent";
    public static final String COL_ID = "id";
    public static final String COL_DIRECTION = "direction";
    public static final String COL_UNIT = "unit";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_RELEVANT_ACCOUNT_ID = "relevant_account_id";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_SUBJECT_ID = "subject_id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_AGENT_ID = "agent_id";

    public static final String JOIN_RELEVANT_ACC_ID = "r." + JdbcTreasury.COL_ID;
    public static final String JOIN_RELEVANT_ACC_NAME = "r." + JdbcTreasury.COL_NAME;
    public static final String JOIN_RELEVANT_ACC_CURRENCY_UNIT = "r." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_RELEVANT_ACC_BALANCE = "r." + JdbcTreasury.COL_BALANCE;

    public static final String JOIN_SUBJECT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ID;
    public static final String JOIN_SUBJECT_PARENT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_PARENT_ID;
    public static final String JOIN_SUBJECT_ROOT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ROOT_ID;
    public static final String JOIN_SUBJECT_CHILD_FLAG = "s." + FundsMutationSubjectJdbcRepository.COL_CHILD_FLAG;
    public static final String JOIN_SUBJECT_TYPE = "s." + FundsMutationSubjectJdbcRepository.COL_TYPE;
    public static final String JOIN_SUBJECT_NAME = "s." + FundsMutationSubjectJdbcRepository.COL_NAME;

    public static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    public static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_UNIT, COL_AMOUNT,
            JOIN_RELEVANT_ACC_ID, JOIN_RELEVANT_ACC_NAME, JOIN_RELEVANT_ACC_CURRENCY_UNIT, JOIN_RELEVANT_ACC_BALANCE,
            COL_QUANTITY,
            JOIN_SUBJECT_ID, JOIN_SUBJECT_PARENT_ID, JOIN_SUBJECT_ROOT_ID, JOIN_SUBJECT_CHILD_FLAG, JOIN_SUBJECT_TYPE, JOIN_SUBJECT_NAME,
            COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DIRECTION, COL_UNIT, COL_AMOUNT, COL_RELEVANT_ACCOUNT_ID, COL_QUANTITY, COL_SUBJECT_ID, COL_TIMESTAMP, COL_AGENT_ID
    );

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final JdbcTreasury.AccountRowMapper accountRowMapper = new JdbcTreasury.AccountRowMapper(sqlDialect);
    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final FundsMutationSubjectJdbcRepository.SubjectRowMapper subjectRowMapper;
    private final MutationRowMapper rowMapper = new MutationRowMapper();

    public FundsMutationEventJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        subjectRowMapper = new FundsMutationSubjectJdbcRepository(jdbcTemplateProvider).getRowMapper();
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
    public AgnosticRowMapper<FundsMutationEvent> getRowMapper() {
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
    public ImmutableList<?> decomposeObject(FundsMutationEvent object) {
        checkArgument(object.relevantBalance.id != null, "Relevant account %s without ID", object.relevantBalance);
        checkArgument(object.subject.id.isPresent(), "Subject %s without ID", object.subject);
        checkArgument(object.agent.id.isPresent(), "Agent with name %s without ID", object.agent.name);

        return ImmutableList.of(
                object.amount.isPositive(),
                object.amount.getCurrencyUnit().getNumericCode(), object.amount.getAmount(),
                object.relevantBalance.id,
                object.quantity,
                object.subject.id.getAsLong(),
                object.timestamp,
                object.agent.id.getAsLong()
        );
    }

    @Nullable
    @Override
    public Object extractId(FundsMutationEvent object) {
        return null;
    }


    @Override
    public void registerBenefit(FundsMutationEvent mutationEvent) {
        if (mutationEvent.amount.isNegative()) {
            mutationEvent = negateEvent(mutationEvent);
        }
        Common.insert(this, mutationEvent);
    }

    @Override
    public void registerLoss(FundsMutationEvent mutationEvent) {
        if (mutationEvent.amount.isPositive()) {
            mutationEvent = negateEvent(mutationEvent);
        }
        Common.insert(this, mutationEvent);
    }

    @Nonnull
    private static FundsMutationEvent negateEvent(FundsMutationEvent mutationEvent) {
        return FundsMutationEvent.builder().setFundsMutationEvent(mutationEvent).setAmount(mutationEvent.amount.negated()).build();
    }

    @Override
    public Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        final String sql = sqlDialect.selectSql(
                TABLE_NAME, null, COLS_FOR_SELECT,
                SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "r", COL_RELEVANT_ACCOUNT_ID, JdbcTreasury.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, FundsMutationSubjectJdbcRepository.TABLE_NAME, "s", COL_SUBJECT_ID, FundsMutationSubjectJdbcRepository.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID)
        ) + SqlDialect.getWhereClausePostfix(sqlDialect, limit, Common.translateOrderBy(options));

        return LazyResultSetIterator.stream(
                Common.getRsSupplier(jdbcTemplateProvider, sql, "streamMutationEvents"),
                Common.getMappingSqlFunction(rowMapper, sql, "streamMutationEvents")
        );
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return ImmutableMap.of();
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + " BIGINT " + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DIRECTION + " BOOLEAN, "
                    + COL_UNIT + " INT, "
                    + COL_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_RELEVANT_ACCOUNT_ID + " BIGINT, "
                    + COL_QUANTITY + " INT, "
                    + COL_SUBJECT_ID + " BIGINT, "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + " BIGINT, "
                    + sqlDialect.foreignKey(new String[] {COL_RELEVANT_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_REL_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SUBJECT_ID}, FundsMutationSubjectJdbcRepository.TABLE_NAME, new String[] {FundsMutationSubjectJdbcRepository.COL_ID}, FK_SUBJ)
                + ')';
    }

    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
        };
    }


    private final class MutationRowMapper implements AgnosticRowMapper<FundsMutationEvent> {

        @Override
        public FundsMutationEvent mapRow(ResultSet rs) throws SQLException {
            final int unit = rs.getInt(1);
            final BigDecimal amount = sqlDialect.translateFromDb(rs.getObject(2), BigDecimal.class);
            final Treasury.BalanceAccount account = accountRowMapper.mapRowStartingFrom(3, rs);
            final int quantity = rs.getInt(7);
            final FundsMutationSubject sub = subjectRowMapper.mapRowStartingFrom(8, rs);
            final OffsetDateTime timestamp = sqlDialect.translateFromDb(rs.getObject(14), OffsetDateTime.class);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(15, rs);

            return FundsMutationEvent.builder()
                    .setAmount(Money.of(CurrencyUnit.ofNumericCode(unit), amount))
                    .setRelevantBalance(account)
                    .setQuantity(quantity)
                    .setSubject(sub)
                    .setTimestamp(timestamp)
                    .setAgent(agent)
                    .build();
        }

    }

}
