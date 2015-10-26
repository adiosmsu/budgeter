package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 7:19 PM
 *
 * @author Mikhail Kulikov
 */
final class StoredCurrencyRate extends Stored<UtcDay> {

    final CurrencyUnit first;
    final CurrencyUnit second;
    final BigDecimal rate;

    StoredCurrencyRate(int id, UtcDay obj, CurrencyUnit first, CurrencyUnit second, BigDecimal rate) {
        super(id, obj);
        this.first = first;
        this.second = second;
        this.rate = rate;
    }

    CurrencyRatesProvider.ConversionRate createConversionRate() {
        return new CurrencyRatesProvider.ConversionRate(obj, new CurrencyRatesProvider.ConversionPair(first, second), rate);
    }

}
