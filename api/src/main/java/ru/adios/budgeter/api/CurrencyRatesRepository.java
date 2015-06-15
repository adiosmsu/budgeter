package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 10:02 PM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyRatesRepository extends CurrencyRatesProvider {

    default void addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        addRate(new UtcDay(), from, to, rate);
    }

    void addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate);

}
