package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

    private static final String JOIN_RELEVANT_ACC_ID = "r." + JdbcTreasury.COL_ID;
    private static final String JOIN_RELEVANT_ACC_NAME = "r." + JdbcTreasury.COL_NAME;
    private static final String JOIN_RELEVANT_ACC_CURRENCY_UNIT = "r." + JdbcTreasury.COL_CURRENCY_UNIT;
    private static final String JOIN_RELEVANT_ACC_BALANCE = "r." + JdbcTreasury.COL_BALANCE;
    private static final String JOIN_RELEVANT_ACC_DESC = "r." + JdbcTreasury.COL_DESCRIPTION;

    private static final String JOIN_SUBJECT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ID;
    private static final String JOIN_SUBJECT_PARENT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_PARENT_ID;
    private static final String JOIN_SUBJECT_ROOT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ROOT_ID;
    private static final String JOIN_SUBJECT_CHILD_FLAG = "s." + FundsMutationSubjectJdbcRepository.COL_CHILD_FLAG;
    private static final String JOIN_SUBJECT_TYPE = "s." + FundsMutationSubjectJdbcRepository.COL_TYPE;
    private static final String JOIN_SUBJECT_NAME = "s." + FundsMutationSubjectJdbcRepository.COL_NAME;
    private static final String JOIN_SUBJECT_DESC = "s." + FundsMutationSubjectJdbcRepository.COL_DESCRIPTION;

    private static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    private static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;
    private static final String JOIN_AGENT_DESC = "a." + FundsMutationAgentJdbcRepository.COL_DESCRIPTION;

    private static final SqlDialect.Join JOIN_RELEVANT_ACCOUNT =
            SqlDialect.Join.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "r", COL_RELEVANT_ACCOUNT_ID, JdbcTreasury.COL_ID);
    private static final SqlDialect.Join JOIN_SUBJECT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationSubjectJdbcRepository.TABLE_NAME, "s", COL_SUBJECT_ID, FundsMutationSubjectJdbcRepository.COL_ID);
    private static final SqlDialect.Join JOIN_AGENT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID);

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_UNIT, COL_AMOUNT,
            JOIN_RELEVANT_ACC_ID, JOIN_RELEVANT_ACC_NAME, JOIN_RELEVANT_ACC_CURRENCY_UNIT, JOIN_RELEVANT_ACC_BALANCE, JOIN_RELEVANT_ACC_DESC,
            COL_QUANTITY,
            JOIN_SUBJECT_ID, JOIN_SUBJECT_PARENT_ID, JOIN_SUBJECT_ROOT_ID, JOIN_SUBJECT_CHILD_FLAG, JOIN_SUBJECT_TYPE, JOIN_SUBJECT_NAME, JOIN_SUBJECT_DESC,
            COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME, JOIN_AGENT_DESC
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DIRECTION, COL_UNIT, COL_AMOUNT, COL_RELEVANT_ACCOUNT_ID, COL_QUANTITY, COL_SUBJECT_ID, COL_TIMESTAMP, COL_AGENT_ID
    );

    private static final String COUNT_ALL_SQL = SqlDialect.Static.countAllSql(TABLE_NAME);
    private static final String SQL_STREAM_START = SqlDialect.Static.selectSqlBuilder(
            TABLE_NAME, COLS_FOR_SELECT,
            JOIN_RELEVANT_ACCOUNT,
            JOIN_SUBJECT,
            JOIN_AGENT
    ).toString();
    private static final String SQL_STREAM_FOR_DAY = getStreamForDaySql();
    private static String getStreamForDaySql() {
        final StringBuilder builder = new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START);
        SqlDialect.Static.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.MORE_EQ, COL_TIMESTAMP);
        SqlDialect.Static.appendWhereClausePart(false, builder, true, SqlDialect.Op.LESS, COL_TIMESTAMP);
        return builder.toString();
    }


    private final SafeJdbcConnector jdbcConnector;
    private final JdbcRepository.Default<FundsMutationEvent> def = new JdbcRepository.Default<FundsMutationEvent>(this);
    private final FundsMutationEventRepository.Default repoDef = new FundsMutationEventRepository.Default(this);

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final JdbcTreasury.AccountRowMapper accountRowMapper = new JdbcTreasury.AccountRowMapper(sqlDialect);
    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final FundsMutationSubjectJdbcRepository subjRepo;
    private final MutationRowMapper rowMapper = new MutationRowMapper();
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = def.getInsertSql(false);

    FundsMutationEventJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this(jdbcConnector, new FundsMutationSubjectJdbcRepository(jdbcConnector));
    }

    FundsMutationEventJdbcRepository(SafeJdbcConnector jdbcConnector, FundsMutationSubjectJdbcRepository subjRepo) {
        this.jdbcConnector = jdbcConnector;
        this.subjRepo = subjRepo;
    }


    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<FundsMutationEvent> getById(Long id) {
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
    public AgnosticRowMapper<FundsMutationEvent> getRowMapper() {
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
    public ImmutableList<?> decomposeObject(FundsMutationEvent object) {
        checkArgument(object.relevantBalance.id.isPresent(), "Relevant account %s without ID", object.relevantBalance);
        checkArgument(object.subject.id.isPresent(), "Subject %s without ID", object.subject);
        checkArgument(object.agent.id.isPresent(), "Agent with name %s without ID", object.agent.name);

        return ImmutableList.of(
                object.amount.isPositive(),
                object.amount.getCurrencyUnit().getNumericCode(), object.amount.getAmount(),
                object.relevantBalance.id.get(),
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
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(FundsMutationEvent object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(FundsMutationEvent object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public void register(FundsMutationEvent mutationEvent) {
        Common.insert(this, mutationEvent);
    }

    @Override
    public int countMutationEvents() {
        return Common.getSingleColumn(this, COUNT_ALL_SQL, Common.INTEGER_ROW_MAPPER);
    }

    @Override
    public Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        final StringBuilder sb = new StringBuilder(SQL_STREAM_START.length() + options.size() * 20 + 15).append(SQL_STREAM_START);
        SqlDialect.Static.appendWhereClausePostfix(sb, sqlDialect, limit.orElse(null), Common.translateOrderBy(options));
        final String sql = sb.toString();

        return LazyResultSetIterator.stream(
                Common.getRsSupplier(jdbcConnector, sql, "streamMutationEvents"),
                Common.getMappingSqlFunction(rowMapper, sql, "streamMutationEvents")
        );
    }

    @Override
    public Stream<FundsMutationEvent> streamForDay(UtcDay day) {
        return Common.streamForDay(this, SQL_STREAM_FOR_DAY, day, "streamForDay");
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return ImmutableMap.of();
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return repoDef.getStatsInTimePeriod(from, till);
    }

    @Override
    public Stream<FundsMutationEvent> streamMutationEvents(RepoOption... options) {
        return repoDef.streamMutationEvents(options);
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                + COL_DIRECTION + " BOOLEAN, "
                + COL_UNIT + " INT, "
                + COL_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                + COL_RELEVANT_ACCOUNT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + COL_QUANTITY + " INT, "
                + COL_SUBJECT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                + COL_AGENT_ID + ' ' + sqlDialect.bigIntType() + ", "
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
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.Static.dropTableCommand(TABLE_NAME)
        };
    }

    @Override
    public void bootstrap(Logger logger) {}

    private final class MutationRowMapper extends AgnosticRowMapper<FundsMutationEvent> {

        @Override
        public FundsMutationEvent mapRow(ResultSet rs) throws SQLException {
            final int unit = rs.getInt(1);
            final BigDecimal amount = sqlDialect.translateFromDb(rs.getObject(2), BigDecimal.class);
            final BalanceAccount account = accountRowMapper.mapRowStartingFrom(3, rs);
            final int quantity = rs.getInt(8);
            final FundsMutationSubject sub = subjRepo.getRowMapper().mapRowStartingFrom(9, rs);
            final OffsetDateTime timestamp = sqlDialect.translateFromDb(rs.getObject(16), OffsetDateTime.class);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(17, rs);

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
