package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.concurrent.ThreadSafe;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 10/26/15
 * Time: 8:25 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class FundsMutationAgentJdbcRepository implements FundsMutationAgentRepository {

    public static final String TABLE_NAME = "funds_mutation_agent";
    public static final String INDEX_NAME = "ix_funds_mutation_agent_name";
    public static final String ID_COL = "id";
    public static final String NAME_COL = "name";

    private static final AgentRowMapper AGENT_ROW_MAPPER = new AgentRowMapper();

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    FundsMutationAgentJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplateProvider.get().update(con -> {
            final PreparedStatement statement = con.prepareStatement(sqlDialect.insertSql(TABLE_NAME, NAME_COL), Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, agent.name);
            return statement;
        }, keyHolder);

        return FundsMutationAgent.builder().setName(agent.name).build();
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        final String sql = sqlDialect.selectSql(TABLE_NAME, null, NAME_COL);
        return LazyResultSetIterator.stream(() -> {
            try {
                return jdbcTemplateProvider.get().getDataSource().getConnection().prepareStatement(sql).executeQuery();
            } catch (SQLException e) {
                throw Common.EXCEPTION_TRANSLATOR.translate("streamAll", sql, e);
            }
        }, resultSet -> {
            try {
                return FundsMutationAgent.builder().setName(resultSet.getString(1)).build();
            } catch (SQLException e) {
                throw Common.EXCEPTION_TRANSLATOR.translate("streamAll", sql, e);
            }
        });
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        final String sql = sqlDialect.selectSql(TABLE_NAME, SqlDialect.generateWhereClause(true, "=", NAME_COL), NAME_COL) + " LIMIT 1";
        final List<FundsMutationAgent> results = jdbcTemplateProvider.get().query(sql, AGENT_ROW_MAPPER, name);
        return Common.getSingleOptional(results);
    }

    @Override
    public Optional<FundsMutationAgent> getById(Long id) {
        final String sql = sqlDialect.selectSql(TABLE_NAME, SqlDialect.generateWhereClause(true, "=", ID_COL), NAME_COL);
        final List<FundsMutationAgent> results = jdbcTemplateProvider.get().query(sql, AGENT_ROW_MAPPER, id);
        return Common.getSingleOptional(results);
    }

    @Override
    public Long currentSeqValue() {
        return jdbcTemplateProvider.get().queryForObject(sqlDialect.sequenceCurrentValueSql(TABLE_NAME, null), Common.LONG_ROW_MAPPER, TABLE_NAME);
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + ID_COL + ' ' + sqlDialect.integerType() + ' ' + sqlDialect.primaryKeyWithNextValue(null) + ", " + NAME_COL + ' ' + sqlDialect.textType() + ')';
    }

    private String getCreateIndexSql() {
        return sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, NAME_COL);
    }

    String[] getCreateTableSql() {
        return new String[] {getActualCreateTableSql(), getCreateIndexSql()};
    }

    private static final class AgentRowMapper implements RowMapper<FundsMutationAgent> {

        @Override
        public FundsMutationAgent mapRow(ResultSet rs, int rowNum) throws SQLException {
            final String name = Common.STRING_ROW_MAPPER.mapRow(rs, rowNum);
            if (name == null) {
                return null;
            }
            return FundsMutationAgent.builder().setName(name).build();
        }

    }

}
