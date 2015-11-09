package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.data.BalanceAccount;

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
public interface Treasury extends Provider<BalanceAccount, Long>, Repository<Long> {

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

    BalanceAccount registerBalanceAccount(BalanceAccount account);

    Stream<CurrencyUnit> streamRegisteredCurrencies();

    Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit);

    Stream<BalanceAccount> streamRegisteredAccounts();

    BalanceAccount getAccountWithId(BalanceAccount account);

    Optional<BalanceAccount> getAccountForName(String accountName);

    static BalanceAccount getTransitoryAccount(CurrencyUnit unit, Treasury treasury) {
        final BalanceAccount account = new BalanceAccount("Транзитный счет для " + unit.getCurrencyCode(), unit, null);
        final Optional<BalanceAccount> accountForName = treasury.getAccountForName(account.name);
        if (!accountForName.isPresent()) {
            return treasury.registerBalanceAccount(account);
        } else {
            return accountForName.get();
        }
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
