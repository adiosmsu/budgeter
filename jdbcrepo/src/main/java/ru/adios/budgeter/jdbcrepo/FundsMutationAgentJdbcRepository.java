package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

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

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_NAME);
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
        Common.insert(this, agent);
        return agent;
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        return Common.streamRequestAll(this, "streamAll");
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        return Common.getByOneUniqueColumn(name, COL_NAME, this);
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + " BIGINT " + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", " + COL_NAME + ' ' + sqlDialect.textType() + ')';
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

    static final class AgentRowMapper implements AgnosticRowMapper<FundsMutationAgent> {

        @Override
        public FundsMutationAgent mapRow(ResultSet rs) throws SQLException {
            final String name = Common.STRING_ROW_MAPPER.mapRow(rs, 0);
            if (name == null) {
                return null;
            }
            return FundsMutationAgent.builder().setName(name).build();
        }

    }


}
