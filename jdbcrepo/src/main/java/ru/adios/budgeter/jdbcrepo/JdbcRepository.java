package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.core.PreparedStatementCreator;
import ru.adios.budgeter.api.Provider;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Date: 10/27/15
 * Time: 3:28 PM
 *
 * @author Mikhail Kulikov
 */
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

    SafeJdbcTemplateProvider getTemplateProvider();

    String getTableName();

    String getIdColumnName();

    String getSeqName();

    ImmutableList<String> getColumnNames();

    default ImmutableList<String> getColumnNames(boolean withId) {
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

    ImmutableList<?> decomposeObject(ObjType object);

    @Nullable
    Object extractId(ObjType object);

    @Override
    default Long currentSeqValue() {
        final SqlDialect sqlDialect = getSqlDialect();
        return Common.getSingleColumn(this, sqlDialect.sequenceCurrentValueSql(getTableName(), getSeqName()), sqlDialect.getRowMapperForType(Long.class));
    }

    @Override
    default Optional<ObjType> getById(Long id) {
        return Common.getByOneColumn(id, getIdColumnName(), this);
    }

    final class InsertStatementCreator<ObjType> implements PreparedStatementCreator {

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
            final ImmutableList<String> columnNames = repo.getColumnNames(withId);

            final SqlDialect sqlDialect = repo.getSqlDialect();
            final PreparedStatement statement =
                    con.prepareStatement(sqlDialect.insertSql(repo.getTableName(), columnNames), PreparedStatement.RETURN_GENERATED_KEYS);

            int i = 1;
            if (withId) {
                statement.setObject(i++, repo.extractId(object));
            }
            for (final Object o : repo.decomposeObject(object)) {
                statement.setObject(i++,
                        sqlDialect.translateForDb(o)
                );
            }

            return statement;
        }

    }

}
