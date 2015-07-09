package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.util.Optional;

/**
 * Date: 6/13/15
 * Time: 2:00 AM
 *
 * @author Mikhail Kulikov
 */
public interface BalancesRepository {

    Optional<Money> amount(CurrencyUnit unit);

    default Money amountForHumans(CurrencyUnit unit) {
        return amount(unit).orElse(Money.zero(unit));
    }

    Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider);

    void addAmount(Money amount);

}
