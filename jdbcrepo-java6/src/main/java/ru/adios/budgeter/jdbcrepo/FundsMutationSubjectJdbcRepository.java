package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import java8.util.stream.Stream;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 10/27/15
 * Time: 3:08 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class FundsMutationSubjectJdbcRepository implements FundsMutationSubjectRepository, JdbcRepository<FundsMutationSubject> {

    public static final String TABLE_NAME = "funds_mutation_subject";
    public static final String SEQ_NAME = "seq_funds_mutation_subject";
    public static final String INDEX_NAME = "ix_funds_mutation_subject_name";
    public static final String INDEX_PARENT = "ix_funds_mutation_subject_parent";
    public static final String COL_ID = "id";
    public static final String COL_PARENT_ID = "parent_id";
    public static final String COL_ROOT_ID = "root_id";
    public static final String COL_CHILD_FLAG = "child_flag";
    public static final String COL_TYPE = "type";
    public static final String COL_NAME = "name";

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_ID, COL_PARENT_ID, COL_ROOT_ID, COL_CHILD_FLAG, COL_TYPE, COL_NAME);

    private static final String SQL_UPDATE_CHILD_FLAG = SqlDialect.Static.getUpdateSqlStandard(TABLE_NAME, ImmutableList.of(COL_CHILD_FLAG), ImmutableList.of(COL_ID));


    private final JdbcRepository.Default<FundsMutationSubject> def = new JdbcRepository.Default<FundsMutationSubject>(this);
    private final FundsMutationSubjectRepository.Default repoDef = new FundsMutationSubjectRepository.Default(this);
    private final SafeJdbcConnector jdbcConnector;
    private final SubjectRowMapper rowMapper = new SubjectRowMapper();
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;
    private final LazySupplier supIdSql = new LazySupplier();
    private final LazySupplier supFindByName = new LazySupplier();
    private final LazySupplier supFindByParent = new LazySupplier();
    private final LazySupplier supStreamAll = new LazySupplier();
    private final LazySupplier supNameLikeSearch = new LazySupplier();
    private final String insertSqlFalse = def.getInsertSql(false);
    private final String insertSqlTrue = def.getInsertSql(true);

    FundsMutationSubjectJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }


    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<FundsMutationSubject> getById(Long id) {
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
    public ImmutableList<String> getColumnNames() {
        return COLS;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return Common.EMPTY_JOINS;
    }

    @Override
    public ImmutableList<?> decomposeObject(FundsMutationSubject object) {
        return ImmutableList.of(object.parentId, object.rootId, object.childFlag, object.type.ordinal(), object.name);
    }

    @Nullable
    @Override
    public Object extractId(FundsMutationSubject object) {
        return object.id.isPresent()
                ? object.id.getAsLong()
                : null;
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return withId ? insertSqlTrue : insertSqlFalse;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(FundsMutationSubject object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(FundsMutationSubject object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public SubjectRowMapper getRowMapper() {
        return rowMapper;
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
    public Optional<FundsMutationSubject> findByName(String name) {
        return Common.getByOneUniqueColumn(name, COL_NAME, this, supFindByName);
    }

    @Override
    public Stream<FundsMutationSubject> findByParent(long parentId) {
        return Common.streamRequest(this, supFindByParent, ImmutableMap.<String, Object>of(COL_PARENT_ID, parentId), "findByParent");
    }

    @Override
    public Stream<FundsMutationSubject> streamAll() {
        return Common.streamRequestAll(this, supStreamAll, "streamAll");
    }

    @Override
    public ImmutableList<FundsMutationSubject> nameLikeSearch(String str) {
        return ImmutableList.copyOf(Common.getByOneColumnList(str, COL_NAME, this, supNameLikeSearch, SqlDialect.Op.LIKE));
    }

    @Override
    public FundsMutationSubject rawAddition(FundsMutationSubject subject) {
        if (subject.id.isPresent()) {
            Common.insertWithId(this, subject);
            return subject;
        }

        final GeneratedKeyHolder keyHolder = Common.insert(this, subject);
        return FundsMutationSubject.builder(this)
                .setFundsMutationSubject(subject)
                .setId(keyHolder.getKey().longValue())
                .build();
    }

    @Override
    public void updateChildFlag(long id) {
        jdbcConnector.getJdbcTemplate().update(SQL_UPDATE_CHILD_FLAG, true, id);
    }

    @Override
    public FundsMutationSubject addSubject(FundsMutationSubject subject) {
        return repoDef.addSubject(subject);
    }

    @Override
    public long getIdForRateSubject() {
        return repoDef.getIdForRateSubject();
    }


    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, COL_NAME),
                sqlDialect.createIndexSql(INDEX_PARENT, TABLE_NAME, false, COL_PARENT_ID)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.Static.dropIndexCommand(INDEX_NAME),
                SqlDialect.Static.dropIndexCommand(INDEX_PARENT),
                SqlDialect.Static.dropTableCommand(TABLE_NAME)
        };
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                + COL_PARENT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + COL_ROOT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + COL_CHILD_FLAG + " BOOLEAN, "
                + COL_TYPE + " INT, "
                + COL_NAME + ' ' + sqlDialect.textType()
                + ')';
    }


    final class SubjectRowMapper extends AgnosticPartialRowMapper<FundsMutationSubject> {

        @Override
        public FundsMutationSubject mapRowStartingFrom(int start, ResultSet rs) throws SQLException {
            final long id = rs.getLong(start++);
            final long parentId = rs.getLong(start++);
            final long rootId = rs.getLong(start++);
            final boolean childFlag = rs.getBoolean(start++);
            final int type = rs.getInt(start++);
            final String name = rs.getString(start);

            if (name == null) {
                return null;
            }

            return FundsMutationSubject.builder(FundsMutationSubjectJdbcRepository.this)
                    .setId(id)
                    .setParentId(parentId)
                    .setRootId(rootId)
                    .setChildFlag(childFlag)
                    .setType(FundsMutationSubject.Type.values()[type])
                    .setName(name)
                    .build();
        }

    }

}
