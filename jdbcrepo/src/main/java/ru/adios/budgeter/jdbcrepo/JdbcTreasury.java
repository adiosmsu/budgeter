package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Treasury.BalanceAccount;

import javax.annotation.Nullable;
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
public class JdbcTreasury implements Treasury, JdbcRepository<BalanceAccount> {

    public static final String TABLE_NAME = "balance_account";
    public static final String SEQ_NAME = "seq_balance_account";
    public static final String INDEX_NAME = "ix_balance_account_name";
    public static final String INDEX_UNIT = "ix_balance_account_unit";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_CURRENCY_UNIT = "currency_unit";
    public static final String COL_BALANCE = "balance";

    private static final ImmutableList<String> COLS = ImmutableList.of(COL_ID, COL_NAME, COL_CURRENCY_UNIT, COL_BALANCE);

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;
    private final AccountRowMapper rowMapper = new AccountRowMapper(sqlDialect);

    public JdbcTreasury(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
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
        return COLS;
    }

    @Override
    public ImmutableList<?> decomposeObject(BalanceAccount object) {
        final Money balance = object.getBalance();
        return ImmutableList.of(object.name, object.getUnit().getNumericCode(), balance != null ? balance.getAmount() : BigDecimal.ZERO);
    }

    @Nullable
    @Override
    public Object extractId(BalanceAccount object) {
        return object.id;
    }


    @Override
    public void setSequenceValue(Long value) {
        jdbcTemplateProvider.get().update(sqlDialect.sequenceSetValueSql(TABLE_NAME, SEQ_NAME), value);
    }


    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        final Optional<BigDecimal> val = Common.getSingleColumnOptional(
                this,
                SqlDialect.selectSql(TABLE_NAME, SqlDialect.generateWhereClausePart(true, SqlDialect.Op.EQUAL, COL_CURRENCY_UNIT), "sum(" + COL_BALANCE + ')'),
                sqlDialect.getRowMapperForType(BigDecimal.class),
                unit.getNumericCode()
        );

        return getMoneyFromVal(unit, val);
    }

    public static Optional<Money> getMoneyFromVal(CurrencyUnit unit, Optional<BigDecimal> val) {
        if (val.isPresent()) {
            return Optional.of(Money.of(unit, val.get()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Money> accountBalance(String accountName) {
        return Common.getSingleColumnOptional(
                this,
                SqlDialect.selectSql(TABLE_NAME, SqlDialect.generateWhereClausePart(true, SqlDialect.Op.EQUAL, COL_NAME), COL_CURRENCY_UNIT, COL_BALANCE),
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
        final String sql = SqlDialect.getUpdateSqlStandard(TABLE_NAME, ImmutableList.of(COL_BALANCE), ImmutableList.of(COL_NAME))
                .replace(COL_BALANCE + " = ?", COL_BALANCE + " = " + COL_BALANCE + " + ?");
        jdbcTemplateProvider.get().update(sql, sqlDialect.translateForDb(amount.getAmount()), accountName);
    }

    @Override
    public void registerBalanceAccount(BalanceAccount account) {
        Common.insert(this, account);
    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        final String sql = "SELECT DISTINCT " + COL_CURRENCY_UNIT + " FROM " + TABLE_NAME;
        final String opName = "streamRegisteredCurrencies";
        return LazyResultSetIterator.stream(
                Common.getRsSupplier(jdbcTemplateProvider, sql, opName),
                Common.getMappingSqlFunction(rs -> CurrencyUnit.ofNumericCode(rs.getInt(1)), sql, opName)
        );
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit) {
        return Common.streamRequest(this, ImmutableMap.of(COL_CURRENCY_UNIT, unit.getNumericCode()), "streamAccountsByCurrency");
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return Common.streamRequestAll(this, "streamRegisteredAccounts");
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
        return Common.getByOneColumn(accountName, COL_NAME, this);
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
                    + COL_BALANCE + ' ' + sqlDialect.decimalType()
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

            if (name == null) {
                return null;
            }

            return new BalanceAccount(id, name, Money.of(CurrencyUnit.ofNumericCode(unitCode), balance));
        }

    }

}
