package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 1:15 AM
 *
 * @author Mikhail Kulikov
 */
public final class BalanceElementCore {

    private final Treasury treasury;
    private final CurrencyRatesProvider provider;

    private Optional<CurrencyUnit> totalUnitRef = Optional.empty();

    public BalanceElementCore(Treasury treasury, CurrencyRatesProvider provider) {
        this.treasury = treasury;
        this.provider = provider;
    }

    public void setTotalUnit(CurrencyUnit totalUnit) {
        this.totalUnitRef = Optional.of(totalUnit);
    }

    public Stream<Money> streamIndividualBalances() {
        return treasury.getRegisteredCurrencies().map(unit -> {
            final Optional<Money> amount = treasury.amount(unit);
            return amount.orElse(Money.of(unit, SPECIAL));
        }).filter(money -> !money.getAmount().equals(SPECIAL));
    }

    public Money getTotalBalance() {
        return treasury.totalAmount(totalUnitNonNull(), provider);
    }

    public boolean noTodayRate() {
        final UtcDay today = new UtcDay();
        final CurrencyUnit main = totalUnitNonNull();
        final AtomicBoolean wasSomething = new AtomicBoolean(false);
        final boolean foundNoRateCase = treasury.getRegisteredCurrencies().filter(unit -> {
            wasSomething.set(true);
            return !main.equals(unit) && !provider.getConversionMultiplier(today, main, unit).isPresent();
        }).findFirst().isPresent();
        return !wasSomething.get() || foundNoRateCase;
    }

    private CurrencyUnit totalUnitNonNull() {
        return totalUnitRef.orElse(CurrencyUnit.USD);
    }

    private static final BigDecimal SPECIAL = BigDecimal.valueOf(Long.MIN_VALUE);

}
