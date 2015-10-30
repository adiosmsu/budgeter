package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 9:52 PM
 *
 * @author Mikhail Kulikov
 */
final class Common {

    static ResultSetSupplier getRsSupplier(SafeJdbcConnector jdbcConnector, String sql) {
        return new ResultSetSupplier(jdbcConnector, sql, null);
    }

    static ResultSetSupplier getRsSupplier(SafeJdbcConnector jdbcConnector, String sql, @Nullable String opName) {
        return new ResultSetSupplier(jdbcConnector, sql, opName);
    }

    static ParametrizedResultSetSupplier getRsSupplierWithParams(SafeJdbcConnector jdbcConnector, SqlDialect sqlDialect, String sql, List<?> params) {
        return getRsSupplierWithParams(jdbcConnector, sqlDialect, sql, params, null);
    }

    static ParametrizedResultSetSupplier getRsSupplierWithParams(SafeJdbcConnector jdbcConnector, SqlDialect sqlDialect, String sql, List<?> params, @Nullable String opName) {
        return new ParametrizedResultSetSupplier(jdbcConnector, sqlDialect, sql, params, opName);
    }

    static <ObjType> SqlFunction<ResultSet, ObjType> getMappingSqlFunction(AgnosticRowMapper<ObjType> rowMapper, @Nullable String sql, @Nullable String opName) {
        return SqlFunction.getVerboseFunction(sql, opName, rowMapper::mapRow);
    }

    static final SingleColumnRowMapper<Long> LONG_ROW_MAPPER = new SingleColumnRowMapper<>(Long.class);
    static final SingleColumnRowMapper<String> STRING_ROW_MAPPER = new SingleColumnRowMapper<>(String.class);
    static final SqlDialect.Join[] EMPTY_JOINS = new SqlDialect.Join[] {};

    static final SQLStateSQLExceptionTranslator EXCEPTION_TRANSLATOR = new SQLStateSQLExceptionTranslator();

    static <T> Optional<T> getSingleOptional(List<T> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(results.get(0));
    }

    static <ObjType> GeneratedKeyHolder insert(JdbcRepository<ObjType> repo, ObjType object) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        repo.getJdbcConnector()
                .get()
                .update(repo.getInsertStatementCreator(object), keyHolder);

        return keyHolder;
    }

    static <ObjType> int insertWithId(JdbcRepository<ObjType> repo, ObjType object) {
        final Object id = repo.extractId(object);
        checkArgument(id != null, "Repo returns null id value from object");

        return repo.getJdbcConnector()
                .get()
                .update(repo.getInsertStatementCreatorWithId(object));
    }

    static <ObjType> Stream<ObjType> streamRequestAll(JdbcRepository<ObjType> repo, @Nullable String opName) {
        final String sql = SqlDialect.selectSql(repo.getTableName(), null, repo.getColumnNames(), repo.getJoins());
        return LazyResultSetIterator.stream(
                getRsSupplier(repo.getJdbcConnector(), sql, opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ObjType> Stream<ObjType> streamRequestAll(JdbcRepository<ObjType> repo, List<OrderBy> options, @Nullable OptLimit limit, @Nullable String opName) {
        final StringBuilder builder = getRepoSelectBuilder(repo);
        SqlDialect.appendWhereClausePostfix(builder, repo.getSqlDialect(), limit, options);
        final String sql = builder.toString();

        return LazyResultSetIterator.stream(
                getRsSupplier(repo.getJdbcConnector(), sql, opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ObjType> Stream<ObjType> streamRequest(JdbcRepository<ObjType> repo, ImmutableMap<String, Object> columnToValueMap, @Nullable String opName) {
        final StringBuilder builder = getRepoSelectBuilder(repo);
        SqlDialect.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, ImmutableList.copyOf(columnToValueMap.keySet()));
        final String sql = builder.toString();

        return LazyResultSetIterator.stream(
                getRsSupplierWithParams(repo.getJdbcConnector(), repo.getSqlDialect(), sql, ImmutableList.copyOf(columnToValueMap.values()), opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    private static <ObjType> StringBuilder getRepoSelectBuilder(JdbcRepository<ObjType> repo) {
        return SqlDialect.selectSqlBuilder(repo.getTableName(), repo.getColumnNames(), repo.getJoins());
    }

    static <ColType> ColType getSingleColumn(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) throws IncorrectResultSizeDataAccessException {
        return repo.getJdbcConnector().get().queryForObject(sql, rowMapper, params);
    }

    static <ColType> Optional<ColType> getSingleColumnOptional(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) {
        List<ColType> list = getSingleColumnList(repo, sql, rowMapper, params);
        return getSingleOptional(list);
    }

    static <ColType> List<ColType> getSingleColumnList(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) {
        return repo.getJdbcConnector().get().query(sql, rowMapper, params);
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
                .getJdbcConnector()
                .get()
                .query(sql, repo.getRowMapper(), column);
    }

    private static <ObjType> String innerByOneColumnSql(String columnName, JdbcRepository<ObjType> repo, SqlDialect.Op op, boolean unique) {
        final StringBuilder builder = getRepoSelectBuilder(repo);
        SqlDialect.appendWhereClausePart(builder.append(" WHERE"), true, op, columnName);
        if (unique) {
            builder.append(" LIMIT 1");
        }
        return builder.toString();
    }

    @Nonnull
    static <T extends OrderedField> List<OrderBy> translateOrderBy(List<OrderBy<T>> options) {
        final List<OrderBy> iHateJava = new ArrayList<>(options.size() + 1);
        for (final OrderBy<T> option : options) {
            iHateJava.add(option);
        }
        return iHateJava;
    }


    static final class ParametrizedResultSetSupplier extends ResultSetSupplier {

        private final List<?> params;
        private final SqlDialect sqlDialect;

        ParametrizedResultSetSupplier(SafeJdbcConnector jdbcConnector, SqlDialect sqlDialect, String sql, List<?> params) {
            this(jdbcConnector, sqlDialect, sql, params, null);
        }

        ParametrizedResultSetSupplier(SafeJdbcConnector jdbcConnector, SqlDialect sqlDialect, String sql, List<?> params, @Nullable String op) {
            super(jdbcConnector, sql, op);
            this.sqlDialect = sqlDialect;
            this.params = params;
        }

        @Override
        protected void enrichStatement(PreparedStatement statement) throws SQLException {
            int i = 1;
            for (final Object o : params) {
                statement.setObject(i++, sqlDialect.translateForDb(o));
            }
        }

    }

}
