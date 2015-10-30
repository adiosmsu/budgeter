package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.function.BiConsumer;
import java8.util.function.BinaryOperator;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Date: 6/12/15
 * Time: 4:27 PM
 *
 * @author Mikhail Kulikov
 */
public interface Treasury extends Provider<Treasury.BalanceAccount, Long>, Repository<Long> {

    final class Static {

        public static BalanceAccount getTransitoryAccount(CurrencyUnit unit, Treasury treasury) {
            final BalanceAccount account = new BalanceAccount("Транзитный счет для " + unit.getCurrencyCode(), unit);
            final Optional<BalanceAccount> accountForName = treasury.getAccountForName(account.name);
            if (!accountForName.isPresent()) {
                return treasury.registerBalanceAccount(account);
            } else {
                return accountForName.get();
            }
        }

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
            return treasury.streamRegisteredAccounts().collect(Collectors.of(
                    new Supplier<MoneyWrapper>() {
                        @Override
                        public MoneyWrapper get() {
                            return new MoneyWrapper(Money.zero(unit));
                        }
                    },
                    new BiConsumer<MoneyWrapper, BalanceAccount>() {
                        @Override
                        public void accept(MoneyWrapper w, BalanceAccount otherAccount) {
                            final Optional<Money> amount = treasury.accountBalance(otherAccount.name);

                            if (amount.isPresent()) {
                                final Money money = amount.get();

                                if (money.getCurrencyUnit().equals(unit)) {
                                    w.plus(money);
                                } else {
                                    final CurrencyUnit otherUnit = otherAccount.getUnit();
                                    final BigDecimal multiplier = ratesProvider.getConversionMultiplier(new UtcDay(), otherUnit, unit).orElseGet(new Supplier<BigDecimal>() {
                                        @Override
                                        public BigDecimal get() {
                                            return ratesProvider.getLatestConversionMultiplier(otherUnit, unit);
                                        }
                                    });

                                    if (multiplier == null) {
                                        throw new NoRateException(unit, otherUnit);
                                    }

                                    w.plus(money.convertedTo(unit, multiplier, RoundingMode.HALF_DOWN));
                                }
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

        public Money amountForHumans(CurrencyUnit unit) {
            return treasury.amount(unit).orElse(Money.zero(unit));
        }

        public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
            return Static.calculateTotalAmount(treasury, unit, ratesProvider);
        }

        public Optional<Money> accountBalance(BalanceAccount account) {
            return treasury.accountBalance(account.name);
        }

        public void addAmount(Money amount, BalanceAccount account) {
            treasury.addAmount(amount, account.name);
        }

    }

    Optional<Money> amount(CurrencyUnit unit);

    Money amountForHumans(CurrencyUnit unit); // default in java8

    Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider); // default in java8

    Optional<Money> accountBalance(String accountName);

    Optional<Money> accountBalance(BalanceAccount account); // default in java8

    void addAmount(Money amount, String accountName);

    void addAmount(Money amount, BalanceAccount account); // default in java8

    BalanceAccount registerBalanceAccount(BalanceAccount account);

    Stream<CurrencyUnit> streamRegisteredCurrencies();

    Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit);

    Stream<BalanceAccount> streamRegisteredAccounts();

    BalanceAccount getAccountWithId(BalanceAccount account);

    Optional<BalanceAccount> getAccountForName(String accountName);

    @Immutable
    final class BalanceAccount {

        public final String name;
        @Nullable
        public final Long id;
        @Nullable private final CurrencyUnit unit;
        @Nullable private final Money balance;

        @SuppressWarnings("NullableProblems")
        public BalanceAccount(@Nonnull String name, @Nonnull CurrencyUnit unit) {
            this.name = name;
            this.unit = unit;
            this.balance = null;
            this.id = null;
        }

        @SuppressWarnings("NullableProblems")
        public BalanceAccount(@Nonnull Long id, @Nonnull String name, @Nonnull Money balance) {
            this.name = name;
            this.balance = balance;
            this.unit = null;
            this.id = id;
        }

        public CurrencyUnit getUnit() {
            if (unit != null) {
                return unit;
            } else if (balance != null) {
                return balance.getCurrencyUnit();
            } else {
                throw new IllegalStateException("Both unit and balance are NULL");
            }
        }

        @Nullable
        public Money getBalance() {
            return balance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BalanceAccount that = (BalanceAccount) o;

            return name.equals(that.name)
                    && !(unit != null ? !unit.equals(that.unit) : that.unit != null)
                    && !(balance != null ? !balance.equals(that.balance) : that.balance != null);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (unit != null ? unit.hashCode() : 0);
            result = 31 * result + (balance != null ? balance.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(100);
            builder.append("Treasury.BalanceAccount{name=")
                    .append(name)
                    .append(", currency=")
                    .append(getUnit());
            if (balance != null) {
                builder.append(", balance=")
                        .append(balance.getAmount());
            }
            builder.append('}');
            return builder.toString();
        }

    }

}
