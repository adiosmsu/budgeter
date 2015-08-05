package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.CurrencyRatesPseudoTable;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;

/**
 * Date: 6/16/15
 * Time: 9:59 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesRepositoryMock implements CurrencyRatesRepository {

    private final CurrencyRatesPseudoTable table = Schema.CURRENCY_RATES;
    private final CurrencyRatesRepository.Default crrDef = new CurrencyRatesRepository.Default(this);
    private final CurrencyRatesProvider.Default crpDef = new CurrencyRatesProvider.Default(this);

    @Override
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return table.addRate(dayUtc, from, to, rate);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return table.getConversionMultiplier(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return crpDef.getConversionMultiplierBidirectional(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return crpDef.getConversionMultiplierWithIntermediate(day, from, to, intermediate);
    }

    @Override
    public BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return crpDef.getLatestConversionMultiplier(from, to);
    }

    @Override
    public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return crpDef.getLatestConversionMultiplierWithIntermediate(from, to, intermediate);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        return crpDef.getLatestOptionalConversionMultiplierBidirectional(from, to);
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

    @Override
    public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return crrDef.addTodayRate(from, to, rate);
    }

    void clear() {
        table.clear();
    }

}
