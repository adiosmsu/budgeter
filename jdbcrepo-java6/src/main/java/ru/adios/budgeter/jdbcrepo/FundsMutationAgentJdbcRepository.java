package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.stream.Stream;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.adios.budgeter.api.FundsMutationAgentRepository;
import ru.adios.budgeter.api.data.FundsMutationAgent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 8:25 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class FundsMutationAgentJdbcRepository implements FundsMutationAgentRepository, JdbcRepository<FundsMutationAgent> {

    public static final String TABLE_NAME = "funds_mutation_agent";
    public static final String SEQ_NAME = "seq_funds_mutation_agent";
    public static final String INDEX_NAME = "ix_funds_mutation_agent_name";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_DESCRIPTION = "description";

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_ID, COL_NAME, COL_DESCRIPTION);
    private static final AgentRowMapper AGENT_ROW_MAPPER = new AgentRowMapper();


    private final JdbcRepository.Default<FundsMutationAgent> def = new JdbcRepository.Default<FundsMutationAgent>(this);

    private final SafeJdbcConnector jdbcConnector;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;
    private final LazySupplier supIdSql = new LazySupplier();
    private final LazySupplier supStreamAll = new LazySupplier();
    private final LazySupplier supFindByName = new LazySupplier();
    private final String insertSql = def.getInsertSql(false);

    FundsMutationAgentJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }

    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<FundsMutationAgent> getById(Long id) {
        return def.getById(id);
    }

    @Override
    public ImmutableList<String> getColumnNamesForInsert(boolean withId) {
        return def.getColumnNamesForInsert(withId);
    }

    @Override
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public AgentRowMapper getRowMapper() {
        return AGENT_ROW_MAPPER;
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
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
    public ImmutableList<?> decomposeObject(FundsMutationAgent object) {
        return ImmutableList.of(object.name);
    }

    @Nullable
    @Override
    public Object extractId(FundsMutationAgent object) {
        return null;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(FundsMutationAgent object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(FundsMutationAgent object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        final GeneratedKeyHolder keyHolder = Common.insert(this, agent);
        return FundsMutationAgent.withId(agent, keyHolder.getKey().longValue());
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        return Common.streamRequestAll(this, supStreamAll, "streamAll");
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        return Common.getByOneUniqueColumn(name, COL_NAME, this, supFindByName);
    }

    @Override
    public FundsMutationAgent getAgentWithId(FundsMutationAgent agent) {
        final Optional<FundsMutationAgent> byName = findByName(agent.name);
        checkArgument(byName.isPresent(), "Agent with name %s not found in DB", agent.name);
        return byName.get();
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " ("
                + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                + COL_NAME + ' ' + sqlDialect.textType() + ", "
                + COL_DESCRIPTION + ' ' + sqlDialect.textType()
                + ')';
    }

    private String getCreateIndexSql() {
        return sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, COL_NAME);
    }

    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                getCreateIndexSql()
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.Static.dropIndexCommand(INDEX_NAME),
                SqlDialect.Static.dropTableCommand(TABLE_NAME)
        };
    }

    @Override
    public void bootstrap(Logger logger) {}

    static final class AgentRowMapper extends AgnosticPartialRowMapper<FundsMutationAgent> {

        public FundsMutationAgent mapRowStartingFrom(int start, ResultSet rs) throws SQLException {
            final long id = rs.getLong(start);
            final String name = rs.getString(start + 1);
            final String desc = rs.getString(start + 2);
            if (name == null) {
                return null;
            }
            return FundsMutationAgent.builder().setId(id).setName(name).setDescription(desc).build();
        }

    }


}
