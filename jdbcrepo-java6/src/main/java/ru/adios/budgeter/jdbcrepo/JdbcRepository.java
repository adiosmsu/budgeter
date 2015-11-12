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
import java8.util.function.Supplier;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import ru.adios.budgeter.api.Provider;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Date: 10/27/15
 * Time: 3:28 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
interface JdbcRepository<ObjType> extends Provider<ObjType, Long> {

    final class Default<ObjType> implements Provider<ObjType, Long> {

        private final JdbcRepository<ObjType> jdbcRepository;

        public Default(JdbcRepository<ObjType> jdbcRepository) {
            this.jdbcRepository = jdbcRepository;
        }

        public PreparedStatementCreator getInsertStatementCreator(ObjType object) {
            return new InsertStatementCreator<ObjType>(jdbcRepository, object, false);
        }

        public PreparedStatementCreator getInsertStatementCreatorWithId(ObjType object) {
            return new InsertStatementCreator<ObjType>(jdbcRepository, object, true);
        }

        public ImmutableList<String> getColumnNamesForInsert(boolean withId) {
            ImmutableList<String> columnNames = jdbcRepository.getColumnNames();
            final String idColumnName = jdbcRepository.getIdColumnName();

            if (idColumnName.equals(columnNames.get(0))) {
                if (!withId) {
                    final ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
                    for (int i = 1; i < columnNames.size(); i++) {
                        builder.add(columnNames.get(i));
                    }
                    columnNames = builder.build();
                }
            } else if (withId) {
                final ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
                builder.add(idColumnName);
                builder.addAll(columnNames);
                columnNames = builder.build();
            }

            return columnNames;
        }

        public String getInsertSql(boolean withId) {
            return jdbcRepository.getSqlDialect().insertSql(jdbcRepository.getTableName(), jdbcRepository.getColumnNamesForInsert(withId));
        }

        @Override
        public Long currentSeqValue() {
            final SqlDialect sqlDialect = jdbcRepository.getSqlDialect();
            return Common.getSingleColumn(
                    jdbcRepository,
                    sqlDialect.sequenceCurrentValueSql(jdbcRepository.getTableName(), jdbcRepository.getSeqName()),
                    sqlDialect.getRowMapperForType(Long.class)
            );
        }

        @Override
        public Optional<ObjType> getById(Long id) {
            return Common.getByOneColumn(
                    id,
                    jdbcRepository.getTableName() + '.' + jdbcRepository.getIdColumnName(),
                    jdbcRepository,
                    jdbcRepository.getIdLazySupplier()
            );
        }

    }

    void setSqlDialect(SqlDialect sqlDialect);

    SqlDialect getSqlDialect();

    AgnosticRowMapper<ObjType> getRowMapper();

    PreparedStatementCreator getInsertStatementCreator(ObjType object);

    PreparedStatementCreator getInsertStatementCreatorWithId(ObjType object);

    SafeJdbcConnector getJdbcConnector();

    String getTableName();

    String getIdColumnName();

    String getSeqName();

    ImmutableList<String> getColumnNames();

    ImmutableList<String> getColumnNamesForInsert(boolean withId);

    SqlDialect.Join[] getJoins();

    ImmutableList<?> decomposeObject(ObjType object);

    @Nullable
    Object extractId(ObjType object);

    @Override
    Long currentSeqValue();

    @Override
    Optional<ObjType> getById(Long id);

    LazySupplier getIdLazySupplier();

    String[] getCreateTableSql();

    String[] getDropTableSql();

    void bootstrap(Logger logger);

    String getInsertSql(boolean withId);


    final class Null {
        private Null() {}
    }

    @Immutable
    class InsertStatementCreator<ObjType> implements PreparedStatementCreator {

        private final JdbcRepository<ObjType> repo;
        private final ObjType object;
        private final boolean withId;

        InsertStatementCreator(JdbcRepository<ObjType> repo, ObjType object, boolean withId) {
            this.repo = repo;
            this.object = object;
            this.withId = withId;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            final SqlDialect sqlDialect = repo.getSqlDialect();
            final String sql = repo.getInsertSql(withId);
            final PreparedStatement statement;

            int i = 1;
            if (withId) {
                statement = con.prepareStatement(sql);
                statement.setObject(i++, repo.extractId(object));
            } else {
                statement = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            }

            for (Object o : repo.decomposeObject(object)) {
                if (o instanceof Null) {
                    o = null;
                }
                statement.setObject(i++,
                        sqlDialect.translateForDb(o)
                );
            }

            return statement;
        }

    }

    @ThreadSafe
    final class LazySupplier {

        private final AtomicReference<String> ref = new AtomicReference<String>(null);

        String getOrCompute(Supplier<String> sqlSupplier) {
            String s = ref.get();
            if (s == null) {
                s = sqlSupplier.get();
                ref.compareAndSet(null, s);
            }
            return s;
        }

    }

    final class Static {

        static Object wrapNull(Object o) {
            if (o == null) {
                return new Null();
            }
            return o;
        }

    }

}
