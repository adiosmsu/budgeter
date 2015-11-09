package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 10/27/15
 * Time: 9:17 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class JdbcTreasury implements Treasury, JdbcRepository<BalanceAccount> {

    public static final String TABLE_NAME = "balance_account";
    public static final String SEQ_NAME = "seq_balance_account";
    public static final String INDEX_NAME = "ix_balance_account_name";
    public static final String INDEX_UNIT = "ix_balance_account_unit";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_CURRENCY_UNIT = "currency_unit";
    public static final String COL_BALANCE = "balance";
    public static final String COL_DESCRIPTION = "description";

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_ID, COL_NAME, COL_CURRENCY_UNIT, COL_BALANCE, COL_DESCRIPTION);
    private static final String SQL_AMOUNT = getSupAmountSql();

    private static String getSupAmountSql() {
        final StringBuilder builder = SqlDialect.selectSqlBuilder(TABLE_NAME, null, "sum(" + COL_BALANCE + ')');
        SqlDialect.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_CURRENCY_UNIT);
        return builder.toString();
    }
    private static final String SQL_ACCOUNT_BALANCE = getAccBalSql();
    private static String getAccBalSql() {
        final StringBuilder builder = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_CURRENCY_UNIT, COL_BALANCE);
        SqlDialect.appendWhereClausePart(builder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_NAME);
        return builder.toString();
    }
    private static final String SQL_ADD_AMOUNT =
            SqlDialect.getUpdateSqlStandard(TABLE_NAME, ImmutableList.of(COL_BALANCE), ImmutableList.of(COL_NAME), SqlDialect.Op.ADD, 0);
    private static final String SQL_STREAM_REGISTERED_CURRENCIES = "SELECT DISTINCT " + COL_CURRENCY_UNIT + " FROM " + TABLE_NAME;


    private final SafeJdbcConnector jdbcConnector;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;
    private final AccountRowMapper rowMapper = new AccountRowMapper(sqlDialect);
    private final String seqValSql = sqlDialect.sequenceSetValueSql(TABLE_NAME, SEQ_NAME);
    private final String insertSql = JdbcRepository.super.getInsertSql(false);
    private LazySupplier supStreamAccountsByCur = new LazySupplier();
    private LazySupplier supStreamRegAcc = new LazySupplier();
    private LazySupplier supAccForName = new LazySupplier();
    private LazySupplier supIdSql = new LazySupplier();

    public JdbcTreasury(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
    }


    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
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
    public AgnosticRowMapper<BalanceAccount> getRowMapper() {
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
        return COLS;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return Common.EMPTY_JOINS;
    }

    @Override
    public ImmutableList<?> decomposeObject(BalanceAccount object) {
        return ImmutableList.of(object.name, object.getUnit().getNumericCode(), object.getAmount());
    }

    @Nullable
    @Override
    public Object extractId(BalanceAccount object) {
        return object.id.orElseThrow(() -> new InvalidDataAccessApiUsageException("Extracting id for DB insertion from " + object + " which lacks one"));
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }


    @Override
    public void setSequenceValue(Long value) {
        jdbcConnector.getJdbcTemplate().update(seqValSql, value);
    }


    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        final Optional<BigDecimal> val =
                Common.getSingleColumnOptional(this, SQL_AMOUNT, sqlDialect.getRowMapperForType(BigDecimal.class), unit.getNumericCode());

        if (val.isPresent()) {
            return Optional.of(Money.of(unit, val.get()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Money> accountBalance(String accountName) {
        return Common.getSingleColumnOptional(
                this,
                SQL_ACCOUNT_BALANCE,
                (AgnosticRowMapper<Money>) rs -> {
                    final int unit = rs.getInt(1);
                    final BigDecimal balance = getBigDecimalFromDb(rs, 2);
                    return Money.of(CurrencyUnit.ofNumericCode(unit), balance);
                },
                accountName
        );
    }

    @Override
    public void addAmount(Money amount, String accountName) {
        final Optional<BalanceAccount> accountForName = getAccountForName(accountName);
        if (!accountForName.isPresent()) {
            registerBalanceAccount(new BalanceAccount(accountName, amount.getCurrencyUnit(), null));
        } else {
            final CurrencyUnit unit = accountForName.get().getUnit();
            if (!unit.equals(amount.getCurrencyUnit())) {
                throw new DataIntegrityViolationException("Trying to add " + amount.getCurrencyUnit() + " to " + unit + " account");
            }
        }
        jdbcConnector.getJdbcTemplate().update(SQL_ADD_AMOUNT, sqlDialect.translateForDb(amount.getAmount()), accountName);
    }

    @Override
    public BalanceAccount registerBalanceAccount(BalanceAccount account) {
        final GeneratedKeyHolder keyHolder = Common.insert(this, account);
        return new BalanceAccount(keyHolder.getKey().longValue(), account.name, account.description.orElse(null), Money.of(account.getUnit(), account.getAmount()));
    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        final String opName = "streamRegisteredCurrencies";
        return LazyResultSetIterator.stream(
                Common.getRsSupplier(jdbcConnector, SQL_STREAM_REGISTERED_CURRENCIES, opName),
                Common.getMappingSqlFunction(rs -> CurrencyUnit.ofNumericCode(rs.getInt(1)), SQL_STREAM_REGISTERED_CURRENCIES, opName)
        );
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit) {
        return Common.streamRequest(this, supStreamAccountsByCur, ImmutableMap.of(COL_CURRENCY_UNIT, unit.getNumericCode()), "streamAccountsByCurrency");
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return Common.streamRequestAll(this, supStreamRegAcc, "streamRegisteredAccounts");
    }

    @Override
    public BalanceAccount getAccountWithId(BalanceAccount account) {
        final Optional<BalanceAccount> accountForName = getAccountForName(account.name);
        if (accountForName.isPresent()) {
            return accountForName.get();
        }
        throw new IllegalArgumentException("Account " + account + " not found if database; unable to enrich with ID");
    }

    @Override
    public Optional<BalanceAccount> getAccountForName(String accountName) {
        return Common.getByOneColumn(accountName, COL_NAME, this, supAccForName);
    }


    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, COL_NAME),
                sqlDialect.createIndexSql(INDEX_UNIT, TABLE_NAME, false, COL_CURRENCY_UNIT)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.dropIndexCommand(INDEX_NAME),
                SqlDialect.dropIndexCommand(INDEX_UNIT),
                SqlDialect.dropTableCommand(TABLE_NAME)
        };
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_NAME + ' ' + sqlDialect.textType() + ", "
                    + COL_CURRENCY_UNIT + " INT, "
                    + COL_BALANCE + ' ' + sqlDialect.decimalType() + ", "
                    + COL_DESCRIPTION + ' ' + sqlDialect.textType()
                + ')';
    }

    private BigDecimal getBigDecimalFromDb(ResultSet rs, int position) throws SQLException {
        return sqlDialect.translateFromDb(rs.getObject(position), BigDecimal.class);
    }


    final static class AccountRowMapper implements AgnosticPartialRowMapper<BalanceAccount> {

        private final SqlDialect sqlDialect;

        AccountRowMapper(SqlDialect sqlDialect) {
            this.sqlDialect = sqlDialect;
        }

        public BalanceAccount mapRowStartingFrom(int start, ResultSet rs) throws SQLException {
            final long id = rs.getLong(start);
            final String name = rs.getString(start + 1);
            final int unitCode = rs.getInt(start + 2);
            final BigDecimal balance = sqlDialect.translateFromDb(rs.getObject(start + 3), BigDecimal.class);
            final String desc = rs.getString(start + 4);

            if (name == null) {
                return null;
            }

            return new BalanceAccount(id, name, desc, Money.of(CurrencyUnit.ofNumericCode(unitCode), balance));
        }

    }

}
