package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository.PostponedMutationEvent;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/29/15
 * Time: 4:16 AM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventJdbcRepository implements PostponedFundsMutationEventRepository, JdbcRepository<PostponedMutationEvent> {

    public static final String TABLE_NAME = "postponed_funds_mutation_event";
    public static final String SEQ_NAME = "seq_postponed_funds_mutation_event";
    public static final String FK_REL_ACC = "fk_fme_relevant_acc";
    public static final String FK_SUBJ = "fk_fme_subject";
    public static final String FK_AGENT = "fk_fme_agent";
    public static final String INDEX_DAY = "ix_pce_e_day";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_UNIT = "unit";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_RELEVANT_ACCOUNT_ID = "relevant_account_id";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_SUBJECT_ID = "subject_id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_AGENT_ID = "agent_id";
    public static final String COL_CONVERSION_UNIT = "conversion_unit";
    public static final String COL_CUSTOM_RATE = "custom_rate";

    public static final String JOIN_RELEVANT_ACC_ID = "r." + JdbcTreasury.COL_ID;
    public static final String JOIN_RELEVANT_ACC_NAME = "r." + JdbcTreasury.COL_NAME;
    public static final String JOIN_RELEVANT_ACC_CURRENCY_UNIT = "r." + JdbcTreasury.COL_CURRENCY_UNIT;
    public static final String JOIN_RELEVANT_ACC_BALANCE = "r." + JdbcTreasury.COL_BALANCE;

    public static final String JOIN_SUBJECT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ID;
    public static final String JOIN_SUBJECT_PARENT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_PARENT_ID;
    public static final String JOIN_SUBJECT_ROOT_ID = "s." + FundsMutationSubjectJdbcRepository.COL_ROOT_ID;
    public static final String JOIN_SUBJECT_CHILD_FLAG = "s." + FundsMutationSubjectJdbcRepository.COL_CHILD_FLAG;
    public static final String JOIN_SUBJECT_TYPE = "s." + FundsMutationSubjectJdbcRepository.COL_TYPE;
    public static final String JOIN_SUBJECT_NAME = "s." + FundsMutationSubjectJdbcRepository.COL_NAME;

    public static final String JOIN_AGENT_ID = "a." + FundsMutationAgentJdbcRepository.COL_ID;
    public static final String JOIN_AGENT_NAME = "a." + FundsMutationAgentJdbcRepository.COL_NAME;

    private static final ImmutableList<String> COLS_FOR_SELECT = ImmutableList.of(
            COL_UNIT, COL_AMOUNT,
            JOIN_RELEVANT_ACC_ID, JOIN_RELEVANT_ACC_NAME, JOIN_RELEVANT_ACC_CURRENCY_UNIT, JOIN_RELEVANT_ACC_BALANCE,
            COL_QUANTITY,
            JOIN_SUBJECT_ID, JOIN_SUBJECT_PARENT_ID, JOIN_SUBJECT_ROOT_ID, JOIN_SUBJECT_CHILD_FLAG, JOIN_SUBJECT_TYPE, JOIN_SUBJECT_NAME,
            COL_TIMESTAMP,
            JOIN_AGENT_ID, JOIN_AGENT_NAME,
            COL_CONVERSION_UNIT, COL_CUSTOM_RATE
    );
    private static final ImmutableList<String> COLS_FOR_INSERT = ImmutableList.of(
            COL_DAY, COL_UNIT, COL_AMOUNT, COL_RELEVANT_ACCOUNT_ID, COL_QUANTITY, COL_SUBJECT_ID, COL_TIMESTAMP, COL_AGENT_ID, COL_CONVERSION_UNIT, COL_CUSTOM_RATE
    );


    private final SafeJdbcTemplateProvider jdbcTemplateProvider;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    private final FundsMutationEventJdbcRepository mutationRepo;
    private final PostponedMutationRowMapper rowMapper = new PostponedMutationRowMapper();

    public PostponedFundsMutationEventJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        mutationRepo = new FundsMutationEventJdbcRepository(jdbcTemplateProvider);
    }

    public PostponedFundsMutationEventJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider, FundsMutationEventJdbcRepository mutationRepo) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.mutationRepo = mutationRepo;
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
    public AgnosticRowMapper<PostponedMutationEvent> getRowMapper() {
        return rowMapper;
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
        return COLS_FOR_SELECT;
    }

    @Override
    public ImmutableList<String> getColumnNamesForInsert(boolean withId) {
        return COLS_FOR_INSERT;
    }

    @Override
    public ImmutableList<?> decomposeObject(PostponedMutationEvent object) {
        checkArgument(object.mutationEvent.relevantBalance.id != null, "Relevant account %s without ID", object.mutationEvent.relevantBalance);
        checkArgument(object.mutationEvent.subject.id.isPresent(), "Subject %s without ID", object.mutationEvent.subject);
        checkArgument(object.mutationEvent.agent.id.isPresent(), "Agent with name %s without ID", object.mutationEvent.agent.name);

        return ImmutableList.of(
                new UtcDay(object.mutationEvent.timestamp),
                object.mutationEvent.amount.getCurrencyUnit().getNumericCode(), object.mutationEvent.amount.getAmount(),
                object.mutationEvent.relevantBalance.id,
                object.mutationEvent.quantity,
                object.mutationEvent.subject.id.getAsLong(),
                object.mutationEvent.timestamp,
                object.mutationEvent.agent.id.getAsLong(),
                object.conversionUnit.getNumericCode(),
                object.customRate.orElse(null)
        );
    }

    @Nullable
    @Override
    public Object extractId(PostponedMutationEvent object) {
        return null;
    }


    @Override
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        if (mutationEvent.amount.isNegative()) {
            mutationEvent = FundsMutationEventJdbcRepository.negateEvent(mutationEvent);
        }
        Common.insert(this, new PostponedMutationEvent(mutationEvent, paidUnit, customRate));
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        if (mutationEvent.amount.isPositive()) {
            mutationEvent = FundsMutationEventJdbcRepository.negateEvent(mutationEvent);
        }
        Common.insert(this, new PostponedMutationEvent(mutationEvent, paidUnit, customRate));
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamInner(day, oneOf, secondOf, SqlDialect.Op.MORE);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamInner(day, oneOf, secondOf, SqlDialect.Op.LESS);
    }

    private Stream<PostponedMutationEvent> streamInner(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf, SqlDialect.Op op) {
        final StringBuilder builder = SqlDialect.selectSqlBuilder(
                TABLE_NAME,
                COLS_FOR_SELECT,
                SqlDialect.innerJoin(TABLE_NAME, JdbcTreasury.TABLE_NAME, "r", COL_RELEVANT_ACCOUNT_ID, JdbcTreasury.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, FundsMutationSubjectJdbcRepository.TABLE_NAME, "s", COL_SUBJECT_ID, FundsMutationSubjectJdbcRepository.COL_ID),
                SqlDialect.innerJoin(TABLE_NAME, FundsMutationAgentJdbcRepository.TABLE_NAME, "a", COL_AGENT_ID, FundsMutationAgentJdbcRepository.COL_ID)
        );
        SqlDialect.appendWhereClausePart(true, builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        SqlDialect.appendWhereClausePart(true, builder.append(" AND (("), true, SqlDialect.Op.EQUAL, JOIN_RELEVANT_ACC_CURRENCY_UNIT, COL_CONVERSION_UNIT);
        SqlDialect.appendWhereClausePart(true, builder.append(") OR ("), true, SqlDialect.Op.EQUAL, JOIN_RELEVANT_ACC_CURRENCY_UNIT, COL_CONVERSION_UNIT);
        SqlDialect.appendWhereClausePart(false, builder.append("))"), true, op, COL_AMOUNT);
        final String sql = builder.toString();

        return LazyResultSetIterator.stream(
                Common.getRsSupplierWithParams(
                        jdbcTemplateProvider, sql,
                        ImmutableList.of(day, oneOf.getNumericCode(), secondOf.getNumericCode(), secondOf.getNumericCode(), oneOf.getNumericCode(), 0),
                        "streamRememberedExchanges"
                ),
                Common.getMappingSqlFunction(rowMapper, sql, "streamRememberedExchanges")
        );
    }


    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + " BIGINT " + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                    + COL_UNIT + " INT, "
                    + COL_AMOUNT + ' ' + sqlDialect.decimalType() + ", "
                    + COL_RELEVANT_ACCOUNT_ID + " BIGINT, "
                    + COL_QUANTITY + " INT, "
                    + COL_SUBJECT_ID + " BIGINT, "
                    + COL_TIMESTAMP + ' ' + sqlDialect.timestampType() + ", "
                    + COL_AGENT_ID + " BIGINT, "
                    + COL_CONVERSION_UNIT + " INT, "
                    + COL_CUSTOM_RATE + ' ' + sqlDialect.decimalType() + ", "
                    + sqlDialect.foreignKey(new String[] {COL_RELEVANT_ACCOUNT_ID}, JdbcTreasury.TABLE_NAME, new String[] {JdbcTreasury.COL_ID}, FK_REL_ACC) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_AGENT_ID}, FundsMutationAgentJdbcRepository.TABLE_NAME, new String[] {FundsMutationAgentJdbcRepository.COL_ID}, FK_AGENT) + ", "
                    + sqlDialect.foreignKey(new String[] {COL_SUBJECT_ID}, FundsMutationSubjectJdbcRepository.TABLE_NAME, new String[] {FundsMutationSubjectJdbcRepository.COL_ID}, FK_SUBJ)
                + ')';
    }

    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_DAY, TABLE_NAME, false, COL_DAY)
        };
    }


    private final class PostponedMutationRowMapper implements AgnosticRowMapper<PostponedMutationEvent> {

        @Override
        public PostponedMutationEvent mapRow(ResultSet rs) throws SQLException {
            final FundsMutationEvent fundsMutationEvent = mutationRepo.getRowMapper().mapRow(rs);
            final int conversionUnit = rs.getInt(16);
            final BigDecimal customRate = sqlDialect.translateFromDb(rs.getObject(17), BigDecimal.class);

            return new PostponedMutationEvent(fundsMutationEvent, CurrencyUnit.ofNumericCode(conversionUnit), Optional.ofNullable(customRate));
        }

    }

}
