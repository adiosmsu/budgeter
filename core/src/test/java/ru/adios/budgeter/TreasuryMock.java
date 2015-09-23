package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.inmemrepo.TreasuryPseudoTable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/16/15
 * Time: 9:54 PM
 *
 * @author Mikhail Kulikov
 */
public class TreasuryMock implements Treasury {

    private final TreasuryPseudoTable table = Schema.TREASURY;

    @Override
    public Optional<Money> accountBalance(String accountName) {
        return table.accountBalance(accountName);
    }

    @Override
    public void addAmount(Money amount, String accountName) {
        table.addAmount(amount, accountName);
    }

    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        return table.amount(unit);
    }

    @Override
    public Optional<Money> accountBalance(BalanceAccount account) {
        return table.accountBalance(account);
    }

    @Override
    public void addAmount(Money amount, BalanceAccount account) {
        table.addAmount(amount, account);
    }

    @Override
    public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return table.totalAmount(unit, ratesProvider);
    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        return table.streamRegisteredCurrencies();
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return table.streamRegisteredAccounts();
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit) {
        return table.streamAccountsByCurrency(unit);
    }

    @Override
    public void registerBalanceAccount(BalanceAccount account) {
        table.registerBalanceAccount(account);
    }

    @Override
    public BalanceAccount getAccountWithId(BalanceAccount account) {
        return table.getAccountWithId(account);
    }

    void clear() {
        table.clear();
    }

}
