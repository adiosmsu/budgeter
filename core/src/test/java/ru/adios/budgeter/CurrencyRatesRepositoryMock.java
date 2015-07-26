package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.CurrencyRatesPseudoTable;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Date: 6/16/15
 * Time: 9:59 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesRepositoryMock implements CurrencyRatesRepository {

    private final CurrencyRatesPseudoTable table = Schema.CURRENCY_RATES;

    @Override
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return table.addRate(dayUtc, from, to, rate);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return table.getConversionMultiplier(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return table.getLatestOptionalConversionMultiplier(from, to);
    }

    @Override
    public boolean isRateStale(CurrencyUnit to) {
        return table.isRateStale(to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return table.getConversionMultiplierStraight(day, from, to);
    }

    void clear() {
        table.clear();
    }

}
