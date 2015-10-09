package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Date: 6/12/15
 * Time: 4:27 PM
 *
 * @author Mikhail Kulikov
 */
public interface Treasury {

    Optional<Money> amount(CurrencyUnit unit);

    default Money amountForHumans(CurrencyUnit unit) {
        return amount(unit).orElse(Money.zero(unit));
    }

    default Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return calculateTotalAmount(this, unit, ratesProvider);
    }

    Optional<Money> accountBalance(String accountName);

    default Optional<Money> accountBalance(BalanceAccount account) {
        return accountBalance(account.name);
    }

    void addAmount(Money amount, String accountName);

    default void addAmount(Money amount, BalanceAccount account) {
        addAmount(amount, account.name);
    }

    void registerBalanceAccount(BalanceAccount account);

    Stream<CurrencyUnit> streamRegisteredCurrencies();

    Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit);

    Stream<BalanceAccount> streamRegisteredAccounts();

    BalanceAccount getAccountWithId(BalanceAccount account);

    Optional<BalanceAccount> getAccountForName(String accountName);

    @Immutable
    final class BalanceAccount {

        public final String name;
        @Nullable public final Long id;
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

    static BalanceAccount getTransitoryAccount(CurrencyUnit unit, Treasury treasury) {
        final BalanceAccount account = new BalanceAccount("Транзитный счет для " + unit.getCurrencyCode(), unit);
        if (!treasury.accountBalance(account).isPresent()) {
            treasury.registerBalanceAccount(account);
        }
        return account;
    }

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
        return treasury.streamRegisteredAccounts().collect(Collector.of(
                () -> new MoneyWrapper(Money.zero(unit)),
                (w, otherAccount) -> {
                    final Optional<Money> amount = treasury.accountBalance(otherAccount.name);

                    if (amount.isPresent()) {
                        final Money money = amount.get();

                        if (money.getCurrencyUnit().equals(unit)) {
                            w.plus(money);
                        } else {
                            final CurrencyUnit otherUnit = otherAccount.getUnit();
                            final BigDecimal multiplier = ratesProvider
                                    .getConversionMultiplier(new UtcDay(), otherUnit, unit)
                                    .orElseGet(() -> ratesProvider.getLatestConversionMultiplier(otherUnit, unit));

                            if (multiplier == null) {
                                throw new NoRateException(unit, otherUnit);
                            }

                            w.plus(money.convertedTo(unit, multiplier, RoundingMode.HALF_DOWN));
                        }
                    }
                },
                MoneyWrapper::plus,
                w -> w.m
        ));
    }

}
