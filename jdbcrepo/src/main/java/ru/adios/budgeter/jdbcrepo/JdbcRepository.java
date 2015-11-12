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
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import ru.adios.budgeter.api.Provider;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Date: 10/27/15
 * Time: 3:28 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
interface JdbcRepository<ObjType> extends Provider<ObjType, Long> {

    void setSqlDialect(SqlDialect sqlDialect);

    SqlDialect getSqlDialect();

    AgnosticRowMapper<ObjType> getRowMapper();

    default PreparedStatementCreator getInsertStatementCreator(ObjType object) {
        return new InsertStatementCreator<>(this, object, false);
    }

    default PreparedStatementCreator getInsertStatementCreatorWithId(ObjType object) {
        return new InsertStatementCreator<>(this, object, true);
    }

    SafeJdbcConnector getJdbcConnector();

    String getTableName();

    String getIdColumnName();

    String getSeqName();

    ImmutableList<String> getColumnNames();

    default ImmutableList<String> getColumnNamesForInsert(boolean withId) {
        ImmutableList<String> columnNames = getColumnNames();
        final String idColumnName = getIdColumnName();

        if (idColumnName.equals(columnNames.get(0))) {
            if (!withId) {
                final ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
                for (int i = 1; i < columnNames.size(); i++) {
                    builder.add(columnNames.get(i));
                }
                columnNames = builder.build();
            }
        } else if (withId) {
            final ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
            builder.add(idColumnName);
            builder.addAll(columnNames);
            columnNames = builder.build();
        }

        return columnNames;
    }

    SqlDialect.Join[] getJoins();

    ImmutableList<?> decomposeObject(ObjType object);

    @Nullable
    default Object extractId(ObjType object) {
        return null;
    }

    @Override
    default Long currentSeqValue() {
        final SqlDialect sqlDialect = getSqlDialect();
        return Common.getSingleColumn(this, sqlDialect.sequenceCurrentValueSql(getTableName(), getSeqName()), sqlDialect.getRowMapperForType(Long.class));
    }

    @Override
    default Optional<ObjType> getById(Long id) {
        return Common.getByOneColumn(id, getTableName() + '.' + getIdColumnName(), this, getIdLazySupplier());
    }

    LazySupplier getIdLazySupplier();

    String[] getCreateTableSql();

    String[] getDropTableSql();

    default void bootstrap(Logger logger) {}

    default String getInsertSql(boolean withId) {
        return getSqlDialect().insertSql(getTableName(), getColumnNamesForInsert(withId));
    }


    static Object wrapNull(Object o) {
        if (o == null) {
            return new Null();
        }
        return o;
    }

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

        private final AtomicReference<String> ref = new AtomicReference<>(null);

        String getOrCompute(Supplier<String> sqlSupplier) {
            String s = ref.get();
            if (s == null) {
                s = sqlSupplier.get();
                ref.compareAndSet(null, s);
            }
            return s;
        }

    }

}
