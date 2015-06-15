package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

/**
 * Date: 6/13/15
 * Time: 2:00 AM
 *
 * @author Mikhail Kulikov
 */
public interface BalancesRepository {

    Money amount(CurrencyUnit unit);

    Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider);

    void addAmount(Money amount);

}
