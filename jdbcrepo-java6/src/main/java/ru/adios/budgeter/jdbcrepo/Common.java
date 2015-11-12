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
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Stream;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    static <ObjType> SqlFunction<ResultSet, ObjType> getMappingSqlFunction(final AgnosticRowMapper<ObjType> rowMapper, @Nullable String sql, @Nullable String opName) {
        return SqlFunction.getVerboseFunction(sql, opName, new SqlFunction<ResultSet, ObjType>() {
            @Override
            public ObjType applySql(ResultSet resultSet) throws SQLException {
                return rowMapper.mapRow(resultSet);
            }
        });
    }

    static final SingleColumnRowMapper<Long> LONG_ROW_MAPPER = new SingleColumnRowMapper<Long>(Long.class);
    static final SingleColumnRowMapper<Integer> INTEGER_ROW_MAPPER = new SingleColumnRowMapper<Integer>(Integer.class);
    static final SingleColumnRowMapper<String> STRING_ROW_MAPPER = new SingleColumnRowMapper<String>(String.class);
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
                .getJdbcTemplate()
                .update(repo.getInsertStatementCreator(object), keyHolder);

        return keyHolder;
    }

    static <ObjType> int insertWithId(JdbcRepository<ObjType> repo, ObjType object) {
        final Object id = repo.extractId(object);
        checkArgument(id != null, "Repo returns null id value from object");

        return repo.getJdbcConnector()
                .getJdbcTemplate()
                .update(repo.getInsertStatementCreatorWithId(object));
    }

    static <ObjType> Stream<ObjType> streamRequestAll(final JdbcRepository<ObjType> repo, JdbcRepository.LazySupplier supplyingDelegate, @Nullable String opName) {
        final String sql = supplyingDelegate.getOrCompute(new Supplier<String>() {
            @Override
            public String get() {
                return SqlDialect.Static.selectSql(repo.getTableName(), null, repo.getColumnNames(), repo.getJoins());
            }
        });
        return LazyResultSetIterator.stream(
                getRsSupplier(repo.getJdbcConnector(), sql, opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ObjType> Stream<ObjType> streamRequestAll(JdbcRepository<ObjType> repo, List<OrderBy> options, @Nullable OptLimit limit, @Nullable String opName) {
        final StringBuilder builder = getRepoSelectBuilder(repo);
        SqlDialect.Static.appendWhereClausePostfix(builder, repo.getSqlDialect(), limit, options);
        final String sql = builder.toString();

        return LazyResultSetIterator.stream(
                getRsSupplier(repo.getJdbcConnector(), sql, opName),
                getMappingSqlFunction(repo.getRowMapper(), sql, opName)
        );
    }

    static <ObjType> Stream<ObjType> streamRequest(final JdbcRepository<ObjType> repo, JdbcRepository.LazySupplier supDel, final ImmutableMap<String, Object> colToVal, @Nullable String op) {
        String sql = supDel.getOrCompute(new Supplier<String>() {
            @Override
            public String get() {
                final StringBuilder builder = getRepoSelectBuilder(repo);
                SqlDialect.Static.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, ImmutableList.copyOf(colToVal.keySet()));
                return builder.toString();
            }
        });

        return streamRequest(repo, sql, ImmutableList.copyOf(colToVal.values()), op);
    }

    static <ObjType> Stream<ObjType> streamRequest(JdbcRepository<ObjType> repo, String sql, ImmutableList params, @Nullable String op) {
        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(repo.getJdbcConnector(), repo.getSqlDialect(), sql, params, op),
                Common.getMappingSqlFunction(repo.getRowMapper(), sql, op)
        );
    }

    static <ObjType> Stream<ObjType> streamForDay(JdbcRepository<ObjType> repo, String sql, UtcDay day, @Nullable String op) {
        return streamRequest(repo, sql, ImmutableList.of(day.inner, day.add(1).inner), op);
    }

    private static <ObjType> StringBuilder getRepoSelectBuilder(JdbcRepository<ObjType> repo) {
        return SqlDialect.Static.selectSqlBuilder(repo.getTableName(), repo.getColumnNames(), repo.getJoins());
    }

    static <ColType> ColType getSingleColumn(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) throws IncorrectResultSizeDataAccessException {
        return repo.getJdbcConnector().getJdbcTemplate().queryForObject(sql, rowMapper, params);
    }

    static <ColType> Optional<ColType> getSingleColumnOptional(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) {
        List<ColType> list = getSingleColumnList(repo, sql, rowMapper, params);
        return getSingleOptional(list);
    }

    static <ColType> List<ColType> getSingleColumnList(JdbcRepository repo, String sql, RowMapper<ColType> rowMapper, Object... params) {
        return repo.getJdbcConnector().getJdbcTemplate().query(sql, rowMapper, params);
    }

    static <ObjType> Optional<ObjType> getByOneUniqueColumn(Object column, String columnName, JdbcRepository<ObjType> repo, JdbcRepository.LazySupplier supplyingDelegate) {
        return innerByOneColumn(column, columnName, repo, SqlDialect.Op.EQUAL, true, supplyingDelegate);
    }

    static <ObjType> Optional<ObjType> getByOneColumn(Object column, String columnName, JdbcRepository<ObjType> repo, JdbcRepository.LazySupplier supplyingDelegate) {
        return innerByOneColumn(column, columnName, repo, SqlDialect.Op.EQUAL, false, supplyingDelegate);
    }

    static <ObjType> List<ObjType> getByOneColumnList(Object col, final String colName, final JdbcRepository<ObjType> repo, JdbcRepository.LazySupplier lazySup, final SqlDialect.Op op) {
        String sql = lazySup.getOrCompute(new Supplier<String>() {
            @Override
            public String get() {
                return innerByOneColumnSql(colName, repo, op, false);
            }
        });
        return innerByOneColumnList(col, repo, sql);
    }

    private static <OT> Optional<OT> innerByOneColumn(Object col, final String cn, final JdbcRepository<OT> repo, final SqlDialect.Op op, final boolean u, JdbcRepository.LazySupplier ls) {
        final List<OT> results = innerByOneColumnList(col, repo, ls.getOrCompute(new Supplier<String>() {
            @Override
            public String get() {
                return innerByOneColumnSql(cn, repo, op, u);
            }
        }));
        return getSingleOptional(results);
    }

    private static <ObjType> List<ObjType> innerByOneColumnList(Object column, JdbcRepository<ObjType> repo, String sql) {
        return repo
                .getJdbcConnector()
                .getJdbcTemplate()
                .query(sql, repo.getRowMapper(), column);
    }

    private static <ObjType> String innerByOneColumnSql(String columnName, JdbcRepository<ObjType> repo, SqlDialect.Op op, boolean unique) {
        final StringBuilder builder = getRepoSelectBuilder(repo);
        SqlDialect.Static.appendWhereClausePart(builder.append(" WHERE"), true, op, columnName);
        if (unique) {
            builder.append(" LIMIT 1");
        }
        return builder.toString();
    }

    @Nonnull
    static <T extends OrderedField> List<OrderBy> translateOrderBy(List<OrderBy<T>> options) {
        final List<OrderBy> iHateJava = new ArrayList<OrderBy>(options.size() + 1);
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
