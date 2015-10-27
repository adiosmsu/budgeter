package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;

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
public class JdbcTreasury implements Treasury, JdbcRepository<Treasury.BalanceAccount> {

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
    private final AccountRowMapper rowMapper = new AccountRowMapper();
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

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
        if (balance != null) {
            return ImmutableList.of(object.name, object.getUnit().getNumericCode(), balance);
        }
        throw new IllegalArgumentException("account isn't ready for insertion");
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
        return null;
    }

    @Override
    public Optional<Money> accountBalance(String accountName) {
        return null;
    }

    @Override
    public void addAmount(Money amount, String accountName) {

    }

    @Override
    public void registerBalanceAccount(BalanceAccount account) {

    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        return null;
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit) {
        return null;
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return null;
    }

    @Override
    public BalanceAccount getAccountWithId(BalanceAccount account) {
        return null;
    }

    @Override
    public Optional<BalanceAccount> getAccountForName(String accountName) {
        return null;
    }


    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_NAME, TABLE_NAME, true, COL_NAME),
                sqlDialect.createIndexSql(INDEX_UNIT, TABLE_NAME, false, COL_CURRENCY_UNIT)
        };
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME
                + " (" + COL_ID + " BIGINT " + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_NAME + ' ' + sqlDialect.textType() + ", "
                    + COL_CURRENCY_UNIT + " INT, "
                    + COL_BALANCE + ' ' + sqlDialect.decimalType()
                + ')';
    }


    final class AccountRowMapper implements AgnosticRowMapper<Treasury.BalanceAccount> {

        @Override
        public Treasury.BalanceAccount mapRow(ResultSet rs) throws SQLException {
            final long id = rs.getLong(1);
            final String name = rs.getString(2);
            final int unitCode = rs.getInt(3);
            final BigDecimal balance = sqlDialect.translateFromDb(rs.getObject(4), BigDecimal.class);

            if (name == null) {
                return null;
            }

            return new Treasury.BalanceAccount(id, name, Money.of(CurrencyUnit.ofNumericCode(unitCode), balance));
        }

    }
}
