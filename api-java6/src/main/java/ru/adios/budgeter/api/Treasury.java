package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.function.BiConsumer;
import java8.util.function.BinaryOperator;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Date: 6/12/15
 * Time: 4:27 PM
 *
 * @author Mikhail Kulikov
 */
public interface Treasury extends CurrenciesRepository, BalancesRepository {

    final class Static {

        public static Money calculateTotalAmount(final Treasury treasury, final CurrencyUnit unit, final CurrencyRatesProvider ratesProvider) {
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
            return treasury.getRegisteredCurrencies().collect(Collectors.of(
                    new Supplier<MoneyWrapper>() {
                        @Override
                        public MoneyWrapper get() {
                            return new MoneyWrapper(Money.zero(unit));
                        }
                    },
                    new BiConsumer<MoneyWrapper, CurrencyUnit>() {
                        @Override
                        public void accept(MoneyWrapper w, final CurrencyUnit otherUnit) {
                            final Optional<Money> amount = treasury.amount(otherUnit);
                            if (amount.isPresent()) {
                                w.plus(amount.get().convertedTo(
                                                unit,
                                                ratesProvider.getConversionMultiplier(new UtcDay(), otherUnit, unit).orElseGet(
                                                        new Supplier<BigDecimal>() {
                                                            @Override
                                                            public BigDecimal get() {
                                                                return ratesProvider.getLatestConversionMultiplier(otherUnit, unit);
                                                            }
                                                        }
                                                ),
                                                RoundingMode.HALF_DOWN)
                                );
                            }
                        }
                    },
                    new BinaryOperator<MoneyWrapper>() {
                        @Override
                        public MoneyWrapper apply(MoneyWrapper w, MoneyWrapper w2) {
                            return w.plus(w2);
                        }
                    },
                    new Function<MoneyWrapper, Money>() {
                        @Override
                        public Money apply(MoneyWrapper w) {
                            return w.m;
                        }
                    }
            ));
        }

    }

    final class Default {

        private final Treasury treasury;

        public Default(Treasury treasury) {
            this.treasury = treasury;
        }

        public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
            return Static.calculateTotalAmount(treasury, unit, ratesProvider);
        }

    }

    @Override
    Stream<CurrencyUnit> getRegisteredCurrencies();

    @Override
    void registerCurrency(CurrencyUnit unit);

    @Override
    ImmutableList<CurrencyUnit> searchCurrenciesByString(String str);

    @Override
    Optional<Money> amount(CurrencyUnit unit);

    @Override
    Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider); // default in java8

    @Override
    void addAmount(Money amount);

}
