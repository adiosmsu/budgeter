package ru.adios.budgeter.api;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

/**
 * Date: 6/13/15
 * Time: 2:00 AM
 *
 * @author Mikhail Kulikov
 */
public interface BalancesRepository {

    final class Default {

        private final BalancesRepository balancesRepository;

        public Default(BalancesRepository balancesRepository) {
            this.balancesRepository = balancesRepository;
        }

        Money amountForHumans(CurrencyUnit unit) {
            return balancesRepository.amount(unit).orElse(Money.zero(unit));
        }
    }

    Optional<Money> amount(CurrencyUnit unit);

    Money amountForHumans(CurrencyUnit unit); // default in java8

    Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider);

    void addAmount(Money amount);

}
