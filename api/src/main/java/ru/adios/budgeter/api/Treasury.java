package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.RoundingMode;
import java.util.stream.Collector;

/**
 * Date: 6/12/15
 * Time: 4:27 PM
 *
 * @author Mikhail Kulikov
 */
public interface Treasury extends CurrenciesRepository, BalancesRepository {

    @Override
    ImmutableList<CurrencyUnit> getRegisteredCurrencies();

    @Override
    void registerCurrency(CurrencyUnit unit);

    @Override
    ImmutableList<CurrencyUnit> searchCurrenciesByString(String str);

    @Override
    Money amount(CurrencyUnit unit);

    @Override
    default Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return calculateTotalAmount(this, unit, ratesProvider);
    }

    @Override
    void addAmount(Money amount);

    static Money calculateTotalAmount(Treasury treasury, CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        final class MoneyWrapper {
            private MoneyWrapper(Money m) {
                this.m = m;
            }
            private Money m;
            private void plus(Money m) {
                this.m = this.m.plus(m);
            }
            private MoneyWrapper plus(MoneyWrapper wrapper) {
                plus(wrapper.m);
                return this;
            }
        }
        return treasury.getRegisteredCurrencies().stream().collect(Collector.of(
                () -> new MoneyWrapper(Money.zero(unit)),
                (w, otherUnit) -> {
                    w.plus(
                            treasury.amount(otherUnit)
                                    .convertedTo(
                                            unit,
                                            ratesProvider
                                                    .getConversionMultiplier(new UtcDay(), otherUnit, unit)
                                                    .orElseGet(() -> ratesProvider.getLatestConversionMultiplier(otherUnit, unit)),
                                            RoundingMode.HALF_DOWN
                                    )
                    );
                },
                MoneyWrapper::plus,
                w -> w.m
        ));
    }

}
