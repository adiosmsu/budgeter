package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

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

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_ID, COL_NAME);
    private static final AgentRowMapper AGENT_ROW_MAPPER = new AgentRowMapper();


    private final SafeJdbcTemplateProvider jdbcTemplateProvider;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    FundsMutationAgentJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
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
        return COLS;
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
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        final GeneratedKeyHolder keyHolder = Common.insert(this, agent);
        return FundsMutationAgent.withId(agent, keyHolder.getKey().longValue());
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        return Common.streamRequestAll(this, "streamAll");
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        return Common.getByOneUniqueColumn(name, COL_NAME, this);
    }

    @Override
    public FundsMutationAgent getAgentWithId(FundsMutationAgent agent) {
        final Optional<FundsMutationAgent> byName = findByName(agent.name);
        checkArgument(byName.isPresent(), "Agent with name %s not found in DB", agent.name);
        return byName.get();
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", " + COL_NAME + ' ' + sqlDialect.textType() + ')';
    }

    private String getCreateIndexSql() {
        return sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, COL_NAME);
    }

    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                getCreateIndexSql()
        };
    }

    static final class AgentRowMapper implements AgnosticPartialRowMapper<FundsMutationAgent> {

        public FundsMutationAgent mapRowStartingFrom(int start, ResultSet rs) throws SQLException {
            final long id = rs.getLong(start);
            final String name = rs.getString(start + 1);
            if (name == null) {
                return null;
            }
            return FundsMutationAgent.builder().setId(id).setName(name).build();
        }

    }


}
