package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
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
    public Stream<CurrencyUnit> getRegisteredCurrencies() {
        return table.getRegisteredCurrencies();
    }

    @Override
    public void registerCurrency(CurrencyUnit unit) {
        table.registerCurrency(unit);
    }

    @Override
    public ImmutableList<CurrencyUnit> searchCurrenciesByString(String str) {
        return table.searchCurrenciesByString(str);
    }

    @Override
    public void addAmount(Money amount) {
        table.addAmount(amount);
    }

    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        return table.amount(unit);
    }

    void clear() {
        table.clear();
    }

}
