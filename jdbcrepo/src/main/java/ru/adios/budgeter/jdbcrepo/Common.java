package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 9:52 PM
 *
 * @author Mikhail Kulikov
 */
class Common {

    static ResultSetSupplier getRsSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql) {
        return new ResultSetSupplier(jdbcTemplateProvider, sql, null);
    }

    static ResultSetSupplier getRsSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, @Nullable String opName) {
        return new ResultSetSupplier(jdbcTemplateProvider, sql, opName);
    }

    static ParametrizedResultSetSupplier getRsSupplierWithParams(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, List<?> params) {
        return getRsSupplierWithParams(jdbcTemplateProvider, sql, params, null);
    }

    static ParametrizedResultSetSupplier getRsSupplierWithParams(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, List<?> params, @Nullable String opName) {
        return new ParametrizedResultSetSupplier(jdbcTemplateProvider, sql, params, opName);
    }

    static <ObjType> SqlFunction<ResultSet, ObjType> getMappingSqlFunction(AgnosticRowMapper<ObjType> rowMapper, @Nullable String sql, @Nullable String opName) {
        return SqlFunction.getVerboseFunction(sql, opName, rowMapper::mapRow);
    }

    static final SingleColumnRowMapper<Long> LONG_ROW_MAPPER = new SingleColumnRowMapper<>(Long.class);
    static final SingleColumnRowMapper<String> STRING_ROW_MAPPER = new SingleColumnRowMapper<>(String.class);

    static final SQLStateSQLExceptionTranslator EXCEPTION_TRANSLATOR = new SQLStateSQLExceptionTranslator();

    static <T> Optional<T> getSingleOptional(List<T> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    static void executeMultipleSql(JdbcTemplate jdbcTemplate, String[] createTableSql) {
        for (String sql : createTableSql) {
            if (sql != null) {
                jdbcTemplate.execute(sql);
            }
        }
    }

    static <ObjType> GeneratedKeyHolder insert(JdbcRepository<ObjType> repo, ObjType object) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        repo.getTemplateProvider()
                .get()
                .update(repo.getInsertStatementCreator(object), keyHolder);

        return keyHolder;
    }

    static <ObjType> GeneratedKeyHolder insertWithId(JdbcRepository<ObjType> repo, ObjType object) {
        final Object id = repo.extractId(object);
        checkArgument(id != null, "Repo %s returns null id value from object", repo);

        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        repo.getTemplateProvider()
                .get()
                .update(repo.getInsertStatementCreatorWithId(object), keyHolder);

        return keyHolder;
    }

    static <ObjType> Stream<ObjType> streamRequestAll(JdbcRepository<ObjType> repo, @Nullable String opName) {
        final String sql = repo.getSqlDialect().selectSql(repo.getTableName(), null, repo.getColumnNames());
        return LazyResultSetIterator.stream(
                getRsSupplier(repo.getTemplateProvider(), sql, opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ObjType> Stream<ObjType> streamRequest(JdbcRepository<ObjType> repo, ImmutableMap<String, Object> columnToValueMap, @Nullable String opName) {
        final String sql = repo.getSqlDialect().selectSql(
                repo.getTableName(),
                SqlDialect.generateWhereClause(true, SqlDialect.Op.EQUAL, ImmutableList.copyOf(columnToValueMap.keySet())),
                repo.getColumnNames()
        );
        return LazyResultSetIterator.stream(
                getRsSupplierWithParams(repo.getTemplateProvider(), sql, ImmutableList.copyOf(columnToValueMap.values()), opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ColType> ColType getSingleColumn(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) throws IncorrectResultSizeDataAccessException {
        return repo.getTemplateProvider().get().queryForObject(sql, rowMapper, params);
    }

    static <ColType> Optional<ColType> getSingleColumnOptional(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) {
        List<ColType> list = repo.getTemplateProvider().get().query(sql, rowMapper, params);
        return getSingleOptional(list);
    }

    static <ObjType> Optional<ObjType> getByOneUniqueColumn(Object column, String columnName, JdbcRepository<ObjType> repo) {
        return innerByOneColumn(column, columnName, repo, SqlDialect.Op.EQUAL, true);
    }

    static <ObjType> Optional<ObjType> getByOneColumn(Object column, String columnName, JdbcRepository<ObjType> repo) {
        return innerByOneColumn(column, columnName, repo, SqlDialect.Op.EQUAL, false);
    }

    static <ObjType> List<ObjType> getByOneColumnList(Object column, String columnName, JdbcRepository<ObjType> repo, SqlDialect.Op op) {
        String sql = innerByOneColumnSql(columnName, repo, op, false);
        return innerByOneColumnList(column, repo, sql);
    }

    private static <ObjType> Optional<ObjType> innerByOneColumn(Object column, String columnName, JdbcRepository<ObjType> repo, SqlDialect.Op op, boolean unique) {
        String sql = innerByOneColumnSql(columnName, repo, op, unique);
        final List<ObjType> results = innerByOneColumnList(column, repo, sql);
        return getSingleOptional(results);
    }

    private static <ObjType> List<ObjType> innerByOneColumnList(Object column, JdbcRepository<ObjType> repo, String sql) {
        return repo
                .getTemplateProvider()
                .get()
                .query(sql, repo.getRowMapper(), column);
    }

    private static <ObjType> String innerByOneColumnSql(String columnName, JdbcRepository<ObjType> repo, SqlDialect.Op op, boolean unique) {
        String sql = repo
                .getSqlDialect()
                .selectSql(repo.getTableName(), SqlDialect.generateWhereClause(true, op, columnName), repo.getColumnNames());
        if (unique) {
            sql += " LIMIT 1";
        }
        return sql;
    }


    static class ResultSetSupplier implements Supplier<ResultSet> {

        private final SafeJdbcTemplateProvider jdbcTemplateProvider;
        private final String sql;
        @Nullable
        private final String op;

        ResultSetSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql) {
            this(jdbcTemplateProvider, sql, null);
        }

        ResultSetSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, @Nullable String op) {
            this.jdbcTemplateProvider = jdbcTemplateProvider;
            this.sql = sql;
            this.op = op;
        }

        @Override
        public ResultSet get() {
            try {
                final PreparedStatement statement = jdbcTemplateProvider.get().getDataSource().getConnection().prepareStatement(sql);
                enrichStatement(statement);
                return statement.executeQuery();
            } catch (SQLException e) {
                //noinspection SqlDialectInspection
                throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.get()", sql, e);
            }
        }

        protected void enrichStatement(PreparedStatement statement) throws SQLException {}

    }

    static final class ParametrizedResultSetSupplier extends ResultSetSupplier {

        private final List<?> params;

        ParametrizedResultSetSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, List<?> params) {
            this(jdbcTemplateProvider, sql, params, null);
        }

        ParametrizedResultSetSupplier(SafeJdbcTemplateProvider jdbcTemplateProvider, String sql, List<?> params, @Nullable String op) {
            super(jdbcTemplateProvider, sql, op);
            this.params = params;
        }

        @Override
        protected void enrichStatement(PreparedStatement statement) throws SQLException {
            int i = 1;
            for (final Object o : params) {
                statement.setObject(i++, o);
            }
        }

    }

}
