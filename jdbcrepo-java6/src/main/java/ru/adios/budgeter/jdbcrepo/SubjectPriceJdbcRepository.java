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
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCreator;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 11/9/15
 * Time: 1:15 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPriceJdbcRepository implements SubjectPriceRepository, JdbcRepository<SubjectPrice> {

    public static final String TABLE_NAME = "subject_price";
    public static final String SEQ_NAME = "seq_subject_price";
    public static final String INDEX_UNIQUE = "ix_subject_price_uq";
    public static final String FK_SUBJ = "fk_sp_subject";
    public static final String FK_AGENT = "fk_sp_agent";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_UNIT = "unit";
    public static final String COL_PRICE = "price";
    public static final String COL_SUBJECT_ID = "subject_id";
    public static final String COL_AGENT_ID = "agent_id";

    private static final String JOIN_SUBJECT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ID;
    private static final String JOIN_SUBJECT_PARENT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_PARENT_ID;
    private static final String JOIN_SUBJECT_ROOT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ROOT_ID;
    private static final String JOIN_SUBJECT_CHILD_FLAG = "s." + FundsMutationSubjectJdbcRepository.COL_CHILD_FLAG;
    private static final String JOIN_SUBJECT_TYPE = "s." + FundsMutationSubjectJdbcRepository.COL_TYPE;
    private static final String JOIN_SUBJECT_NAME = "s." + FundsMutationSubjectJdbcRepository.COL_NAME;
    private static final String JOIN_SUBJECT_DESC = "s." + FundsMutationSubjectJdbcRepository.COL_DESCRIPTION;

    private static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    private static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;
    private static final String JOIN_AGENT_DESC = "a." + FundsMutationAgentJdbcRepository.COL_DESCRIPTION;

    private static final SqlDialect.Join JOIN_SUBJECT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationSubjectJdbcRepository.TABLE_NAME, "s", COL_SUBJECT_ID, FundsMutationSubjectJdbcRepository.COL_ID);
    private static final SqlDialect.Join JOIN_AGENT =
            SqlDialect.Join.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID);

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_DAY,
            COL_UNIT, COL_PRICE,
            JOIN_SUBJECT_ID, JOIN_SUBJECT_PARENT_ID, JOIN_SUBJECT_ROOT_ID, JOIN_SUBJECT_CHILD_FLAG, JOIN_SUBJECT_TYPE, JOIN_SUBJECT_NAME, JOIN_SUBJECT_DESC,
            JOIN_AGENT_ID, JOIN_AGENT_NAME, JOIN_AGENT_DESC
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DAY, COL_UNIT, COL_PRICE, COL_SUBJECT_ID, COL_AGENT_ID
    );

    private static final String COUNT_BY_AGENTS_SQL = SqlDialect.Static.appendWhereClausePart(
            SqlDialect.Static.appendJoins(SqlDialect.Static.countSqlBuilder(TABLE_NAME, "*"), new SqlDialect.Join[] {JOIN_SUBJECT, JOIN_AGENT}).append(" WHERE"),
            true,
            SqlDialect.Op.EQUAL,
            JOIN_SUBJECT_NAME, JOIN_AGENT_NAME
    ).toString();
    private static final String COUNT_BY_AGENTS_SQL_FAST = SqlDialect.Static.appendWhereClausePart(
            SqlDialect.Static.countSqlBuilder(TABLE_NAME, "*").append(" WHERE"),
            true,
            SqlDialect.Op.EQUAL,
            COL_SUBJECT_ID, COL_AGENT_ID
    ).toString();
    private static final String COUNT_SQL = SqlDialect.Static.appendWhereClausePart(
            SqlDialect.Static.appendJoins(SqlDialect.Static.countSqlBuilder(TABLE_NAME, "*"), new SqlDialect.Join[] {JOIN_SUBJECT}).append(" WHERE"),
            true,
            SqlDialect.Op.EQUAL,
            JOIN_SUBJECT_NAME
    ).toString();
    private static final String COUNT_SQL_FAST = SqlDialect.Static.appendWhereClausePart(
            SqlDialect.Static.countSqlBuilder(TABLE_NAME, "*").append(" WHERE"),
            true,
            SqlDialect.Op.EQUAL,
            COL_SUBJECT_ID
    ).toString();
    public static final String EXISTS_SQL = SqlDialect.Static.appendWhereClausePart(
            SqlDialect.Static.selectSqlBuilder(TABLE_NAME, ImmutableList.of(COL_ID)).append(" WHERE"),
            true,
            SqlDialect.Op.EQUAL,
            COL_SUBJECT_ID, COL_AGENT_ID, COL_DAY
    ).toString();
    private static final String SQL_STREAM_START = SqlDialect.Static.selectSqlBuilder(
            TABLE_NAME, COLS_FOR_SELECT,
            JOIN_SUBJECT,
            JOIN_AGENT
    ).append(" WHERE").toString();
    private static final String SQL_STREAM_FOR_AGENT_FAST = SqlDialect.Static.appendWhereClausePart(
            new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START),
            true,
            SqlDialect.Op.EQUAL,
            COL_SUBJECT_ID, COL_AGENT_ID
    ).toString();
    private static final String SQL_STREAM_FOR_AGENT = SqlDialect.Static.appendWhereClausePart(
            new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START),
            true,
            SqlDialect.Op.EQUAL,
            JOIN_SUBJECT_NAME, JOIN_AGENT_NAME
    ).toString();
    private static final String SQL_STREAM_FAST = SqlDialect.Static.appendWhereClausePart(
            new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START),
            true,
            SqlDialect.Op.EQUAL,
            COL_SUBJECT_ID
    ).toString();
    private static final String SQL_STREAM = SqlDialect.Static.appendWhereClausePart(
            new StringBuilder(SQL_STREAM_START.length() + 200).append(SQL_STREAM_START),
            true,
            SqlDialect.Op.EQUAL,
            JOIN_SUBJECT_NAME
    ).toString();

    private final SafeJdbcConnector jdbcConnector;
    private final JdbcRepository.Default<SubjectPrice> def = new JdbcRepository.Default<SubjectPrice>(this);
    private final SubjectPriceRepository.Default repoDef = new SubjectPriceRepository.Default(this);

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final FundsMutationAgentJdbcRepository.AgentRowMapper agentRowMapper = new FundsMutationAgentJdbcRepository.AgentRowMapper();
    private final FundsMutationSubjectJdbcRepository subjRepo;
    private final SubjectPriceRowMapper rowMapper = new SubjectPriceRowMapper();
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = def.getInsertSql(false);

    public SubjectPriceJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
        subjRepo = new FundsMutationSubjectJdbcRepository(jdbcConnector);
    }

    public SubjectPriceJdbcRepository(SafeJdbcConnector jdbcConnector, FundsMutationSubjectJdbcRepository subjRepo) {
        this.jdbcConnector = jdbcConnector;
        this.subjRepo = subjRepo;
    }


    @Override
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public AgnosticRowMapper<SubjectPrice> getRowMapper() {
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
    public ImmutableList<String> getColumnNames() {
        return COLS_FOR_SELECT;
    }

    @Override
    public ImmutableList<String> getColumnNamesForInsert(boolean withId) {
        return COLS_FOR_INSERT;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return new SqlDialect.Join[] {
                JOIN_SUBJECT,
                JOIN_AGENT
        };
    }

    @Override
    public Long currentSeqValue() {
        return def.currentSeqValue();
    }

    @Override
    public Optional<SubjectPrice> getById(Long id) {
        return def.getById(id);
    }

    @Override
    public ImmutableList<?> decomposeObject(SubjectPrice object) {
        checkSubjectAndAgent(object.subject, object.agent);

        return ImmutableList.of(
                object.day,
                object.price.getCurrencyUnit().getNumericCode(), object.price.getAmount(),
                object.subject.id.getAsLong(),
                object.agent.id.getAsLong()
        );
    }

    @Nullable
    @Override
    public Object extractId(SubjectPrice object) {
        return null;
    }



    private void checkSubjectAndAgent(FundsMutationSubject subject, FundsMutationAgent agent) {
        checkArgument(subject.id.isPresent(), "Subject %s without ID", subject);
        checkArgument(agent.id.isPresent(), "Agent %s without ID", agent);
    }

    @Override
    public Stream<SubjectPrice> stream(long subjectId, RepoOption... options) {
        return repoDef.stream(subjectId, options);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, RepoOption... options) {
        return repoDef.streamByAgent(subjectId, agentId, options);
    }

    @Override
    public Stream<SubjectPrice> stream(FundsMutationSubject subject, RepoOption... options) {
        return repoDef.stream(subject, options);
    }

    @Override
    public Stream<SubjectPrice> stream(String subjectName, RepoOption... options) {
        return repoDef.stream(subjectName, options);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, RepoOption... options) {
        return repoDef.streamByAgent(subject, agent, options);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, RepoOption... options) {
        return repoDef.streamByAgent(subjectName, agentName, options);
    }

    @Override
    public int count(final FundsMutationSubject subject) {
        return Common.getSingleColumn(
                this,
                COUNT_SQL_FAST,
                Common.INTEGER_ROW_MAPPER,
                subject.id.orElseThrow(new Supplier<IllegalArgumentException>() {
                    @Override
                    public IllegalArgumentException get() {
                        return new IllegalArgumentException("Subject " + subject + " is without id");
                    }
                })
        );
    }

    @Override
    public void register(SubjectPrice subjectPrice) {
        Common.insert(this, subjectPrice);
    }

    @Override
    public boolean priceExists(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day) {
        checkSubjectAndAgent(subject, agent);
        return Common.getSingleColumnOptional(
                this,
                EXISTS_SQL,
                Common.LONG_ROW_MAPPER,
                subject.id.getAsLong(), agent.id.getAsLong(), sqlDialect.translateForDb(day)
        ).isPresent();
    }

    @Override
    public int countByAgent(FundsMutationSubject subject, FundsMutationAgent agent) {
        checkSubjectAndAgent(subject, agent);
        return Common.getSingleColumn(
                this,
                COUNT_BY_AGENTS_SQL_FAST,
                Common.INTEGER_ROW_MAPPER,
                subject.id.getAsLong(), agent.id.getAsLong()
        );
    }

    @Override
    public int countByAgent(String subjectName, String agentName) {
        return Common.getSingleColumn(
                this,
                COUNT_BY_AGENTS_SQL,
                Common.INTEGER_ROW_MAPPER,
                subjectName, agentName
        );
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return innerStream(SQL_STREAM_FOR_AGENT_FAST, options, limit, "streamByAgent", subjectId, agentId);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        checkSubjectAndAgent(subject, agent);
        return innerStream(SQL_STREAM_FOR_AGENT_FAST, options, limit, "streamByAgent", subject.id.getAsLong(), agent.id.getAsLong());
    }

    private Stream<SubjectPrice> innerStream(String sqlStart, List<OrderBy<Field>> options, Optional<OptLimit> limit, String methodName, Object... params) {
        final String sql = SqlDialect.Static.appendWhereClausePostfix(
                new StringBuilder(sqlStart.length() + 100).append(sqlStart),
                sqlDialect,
                limit.orElse(null),
                Common.translateOrderBy(options)
        ).toString();

        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(jdbcConnector, sqlDialect, sql, ImmutableList.copyOf(params), methodName),
                Common.getMappingSqlFunction(rowMapper, sql, methodName)
        );
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return innerStream(SQL_STREAM_FOR_AGENT, options, limit, "streamByAgent", subjectName, agentName);
    }

    @Override
    public int count(String subjectName) {
        return Common.getSingleColumn(
                this,
                COUNT_SQL,
                Common.INTEGER_ROW_MAPPER,
                subjectName
        );
    }

    @Override
    public Stream<SubjectPrice> stream(long subjectId, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return innerStream(SQL_STREAM_FAST, options, limit, "stream", subjectId);
    }

    @Override
    public Stream<SubjectPrice> stream(FundsMutationSubject subject, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return innerStream(SQL_STREAM_FAST, options, limit, "stream", subject.id.getAsLong());
    }

    @Override
    public Stream<SubjectPrice> stream(String subjectName, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return innerStream(SQL_STREAM, options, limit, "stream", subjectName);
    }


    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreator(SubjectPrice object) {
        return def.getInsertStatementCreator(object);
    }

    @Override
    public PreparedStatementCreator getInsertStatementCreatorWithId(SubjectPrice object) {
        return def.getInsertStatementCreatorWithId(object);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME + " ("
                + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                + COL_UNIT + " INT, "
                + COL_PRICE + ' ' + sqlDialect.decimalType() + ", "
                + COL_SUBJECT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + COL_AGENT_ID + ' ' + sqlDialect.bigIntType() + ", "
                + sqlDialect.foreignKey(new String[] {COL_AGENT_ID},
                FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT) + ", "
                + sqlDialect.foreignKey(new String[] {COL_SUBJECT_ID},
                FundsMutationSubjectJdbcRepository.TABLE_NAME, new String[] {FundsMutationSubjectJdbcRepository.COL_ID}, FK_SUBJ)
                + ')';
    }

    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_UNIQUE, TABLE_NAME, true, COL_DAY, COL_SUBJECT_ID, COL_AGENT_ID)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                SqlDialect.Static.dropIndexCommand(INDEX_UNIQUE),
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.Static.dropTableCommand(TABLE_NAME)
        };
    }

    @Override
    public void bootstrap(Logger logger) {}


    private final class SubjectPriceRowMapper extends AgnosticRowMapper<SubjectPrice> {

        @Override
        public SubjectPrice mapRow(ResultSet rs) throws SQLException {
            final UtcDay day = sqlDialect.translateFromDb(rs.getObject(1), UtcDay.class);
            final int unit = rs.getInt(2);
            final BigDecimal price = sqlDialect.translateFromDb(rs.getObject(3), BigDecimal.class);
            final FundsMutationSubject sub = subjRepo.getRowMapper().mapRowStartingFrom(4, rs);
            final FundsMutationAgent agent = agentRowMapper.mapRowStartingFrom(11, rs);

            return SubjectPrice.builder()
                    .setDay(day)
                    .setPrice(Money.of(CurrencyUnit.ofNumericCode(unit), price))
                    .setSubject(sub)
                    .setAgent(agent)
                    .build();
        }

    }

}
