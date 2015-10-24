package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
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
@NotThreadSafe
public final class BalanceElementCore {

    private final Treasury treasury;
    private final CurrencyRatesProvider provider;

    private Optional<CurrencyUnit> totalUnitRef = Optional.empty();

    public BalanceElementCore(Treasury treasury, CurrencyRatesProvider provider) {
        this.treasury = treasury;
        this.provider = provider;
    }

    public void setTotalUnit(CurrencyUnit totalUnit) {
        this.totalUnitRef = Optional.ofNullable(totalUnit);
    }

    @Nullable
    public CurrencyUnit getTotalUnit() {
        return totalUnitRef.orElse(null);
    }

    @PotentiallyBlocking
    public Stream<Money> streamIndividualBalances() {
        return treasury.streamRegisteredCurrencies().map(unit -> {
            final Optional<Money> amount = treasury.amount(unit);
            return amount.orElse(Money.of(unit, SPECIAL));
        }).filter(money -> !money.getAmount().equals(SPECIAL));
    }

    @PotentiallyBlocking
    public Money getTotalBalance() {
        return treasury.totalAmount(totalUnitNonNull(), provider);
    }

    @PotentiallyBlocking
    public boolean noTodayRate() {
        final UtcDay today = new UtcDay();
        final CurrencyUnit main = totalUnitNonNull();
        final AtomicBoolean wasSomething = new AtomicBoolean(false);
        final boolean foundNoRateCase = treasury.streamRegisteredCurrencies().filter(unit -> {
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
