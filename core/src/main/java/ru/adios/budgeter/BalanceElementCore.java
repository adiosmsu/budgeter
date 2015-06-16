package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 1:15 AM
 *
 * @author Mikhail Kulikov
 */
public final class BalanceElementCore {

    private final Treasury treasury;
    private final CurrenciesExchangeService ratesService;

    private Optional<CurrencyUnit> totalUnitRef = Optional.empty();

    public BalanceElementCore(Treasury treasury, CurrenciesExchangeService ratesService) {
        this.treasury = treasury;
        this.ratesService = ratesService;
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
        return treasury.totalAmount(totalUnitNonNull(), ratesService);
    }

    public boolean noTodayRate() {
        final List<CurrencyUnit> collected = treasury.getRegisteredCurrencies().collect(Collectors.toList());
        return collected.stream().collect(Collectors.<CurrencyUnit>averagingInt(unit -> ratesService.isRateStale(unit) ? 0 : 1)) < collected.size()
                || ratesService.isRateStale(totalUnitNonNull());
    }

    private CurrencyUnit totalUnitNonNull() {
        return totalUnitRef.orElse(CurrencyUnit.USD);
    }

    private static final BigDecimal SPECIAL = BigDecimal.valueOf(Long.MIN_VALUE);

}
