package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.BalancesRepository;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.inmemrepo.TreasuryPseudoTable;

/**
 * Date: 6/16/15
 * Time: 9:54 PM
 *
 * @author Mikhail Kulikov
 */
public class TreasuryMock implements Treasury {

    private final TreasuryPseudoTable table = Schema.TREASURY;
    private final Treasury.Default tDef = new Treasury.Default(this);
    private final BalancesRepository.Default brDef = new BalancesRepository.Default(this);

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

    @Override
    public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return tDef.totalAmount(unit, ratesProvider);
    }

    @Override
    public Money amountForHumans(CurrencyUnit unit) {
        return brDef.amountForHumans(unit);
    }

    void clear() {
        table.clear();
    }

}
