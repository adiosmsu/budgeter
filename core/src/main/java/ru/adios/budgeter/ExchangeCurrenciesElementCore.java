package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 6:18 AM
 *
 * @author Mikhail Kulikov
 */
public final class ExchangeCurrenciesElementCore implements FundsMutator, Submitter {

    private final Accounter accounter;
    private final Treasury treasury;
    private final CurrenciesExchangeService ratesService;

    private final MoneyWrapperBean buyAmountWrapper = new MoneyWrapperBean("exchange buy amount");

    private Optional<CurrencyUnit> sellUnitRef = Optional.empty();
    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private Optional<OffsetDateTime> timestampRef = Optional.of(OffsetDateTime.now());

    public ExchangeCurrenciesElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.treasury = treasury;
        this.ratesService = ratesService;
    }

    public void setNaturalRate(BigDecimal naturalRate) {
        this.naturalRateRef = Optional.ofNullable(naturalRate);
    }

    public void setBuyAmount(int coins, int cents) {
        buyAmountWrapper.setAmount(coins, cents);
    }

    public void setBuyAmount(Money buyAmount) {
        buyAmountWrapper.setAmount(buyAmount);
    }

    public void setBuyAmountDecimal(BigDecimal buyAmount) {
        buyAmountWrapper.setAmountDecimal(buyAmount);
    }

    public void setBuyAmountUnit(String buyAmountUnitName) {
        buyAmountWrapper.setAmountUnit(buyAmountUnitName);
    }

    public void setSellUnit(CurrencyUnit sellUnit) {
        this.sellUnitRef = Optional.of(sellUnit);
    }

    public void setCustomRate(BigDecimal customRate) {
        this.customRateRef = Optional.ofNullable(customRate);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestampRef = Optional.of(timestamp);
    }

    @Override
    public void submit() {
        checkState(sellUnitRef.isPresent(), "Sell unit not set");

        final Money buyAmount = buyAmountWrapper.getAmount();

        final CurrencyUnit unitBuy = buyAmount.getCurrencyUnit();
        final CurrencyUnit unitSell = sellUnitRef.get();

        final BigDecimal naturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(), unitBuy, unitSell).orElse(null));
        if (naturalRate == null) {
            // we don't have rates in question for today yet, conserve operation to commit later
            accounter.rememberPostponedExchange(buyAmount, unitSell, customRateRef, timestampRef.get());
            return;
        }

        final Money sellAmount;
        final BigDecimal actualRate;
        if (customRateRef.isPresent()) {
            // that will introduce exchange difference between money hypothetically exchanged by default rate and money exchanged by custom rate
            actualRate = customRateRef.get();

            final Money naturalSellAmount = buyAmount.convertedTo(unitSell, naturalRate, RoundingMode.HALF_DOWN);
            sellAmount = buyAmount.convertedTo(unitSell, actualRate, RoundingMode.HALF_DOWN);
            FundsMutator.registerExchangeDifference(this, naturalSellAmount, sellAmount, MutationDirection.LOSS, 1);
        } else {
            actualRate = naturalRate;
            sellAmount = buyAmount.convertedTo(unitSell, actualRate, RoundingMode.HALF_DOWN);
        }

        accounter.registerCurrencyExchange(
                CurrencyExchangeEvent.builder()
                        .setBought(buyAmount)
                        .setSold(sellAmount)
                        .setRate(actualRate)
                        .setTimestamp(timestampRef.get())
                        .build()
        );
    }

    @Override
    public Accounter getAccounter() {
        return accounter;
    }

    @Override
    public CurrenciesExchangeService getRatesService() {
        return ratesService;
    }

    @Override
    public Treasury getTreasury() {
        return treasury;
    }

}
