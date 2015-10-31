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

    final class Default extends CurrencyRatesProvider.Default {

        private final CurrencyRatesRepository currencyRatesRepository;

        public Default(CurrencyRatesRepository currencyRatesRepository) {
            super(currencyRatesRepository);
            this.currencyRatesRepository = currencyRatesRepository;
        }

        public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
            return currencyRatesRepository.addRate(new UtcDay(), from, to, rate);
        }

    }

    /**
     * Add a today's rate to repository.
     * @param from   from what currency exchange is happening
     * @param to     to what currency exchange is happening
     * @param rate   rate as decimal number
     * @return true if success, false otherwise
     */
    boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate); // default in java8

    /**
     * Add a rate to repository.
     * @param dayUtc day of the rate
     * @param from   from what currency exchange is happening
     * @param to     to what currency exchange is happening
     * @param rate   rate as decimal number
     * @return true if success, false otherwise
     */
    boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate);

}
