package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

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
    private final MoneyWrapperBean sellAmountWrapper = new MoneyWrapperBean("exchange sell amount");

    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private Optional<OffsetDateTime> timestampRef = Optional.of(OffsetDateTime.now());
    private BigDecimal calculatedNaturalRate;

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

    public void setSellAmountUnit(CurrencyUnit sellUnit) {
        sellAmountWrapper.setAmountUnit(sellUnit);
    }

    public void setSellAmountDecimal(BigDecimal sellAmount) {
        sellAmountWrapper.setAmountDecimal(sellAmount);
    }

    public void setSellAmount(Money sellAmount) {
        sellAmountWrapper.setAmount(sellAmount);
    }

    public void setCustomRate(BigDecimal customRate) {
        this.customRateRef = Optional.ofNullable(customRate);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestampRef = Optional.of(timestamp);
    }

    @Override
    public void submit() {
        final CurrencyUnit buyUnit = buyAmountWrapper.getAmountUnit();
        final CurrencyUnit sellUnit = sellAmountWrapper.getAmountUnit();

        if (!customRateRef.isPresent() && buyAmountWrapper.isAmountSet() && sellAmountWrapper.isAmountSet()) {
            customRateRef = Optional.of(buyAmountWrapper.getAmount().getAmount().divide(sellAmountWrapper.getAmount().getAmount(), RoundingMode.HALF_DOWN));
        }

        final Money buyAmount, sellAmount;
        final BigDecimal actualRate = customRateRef.orElseGet(() -> calculateNaturalRate(buyUnit, sellUnit));
        if (!buyAmountWrapper.isAmountSet()) {
            sellAmount = sellAmountWrapper.getAmount();
            buyAmount = sellAmount.convertedTo(buyUnit, actualRate, RoundingMode.HALF_DOWN);
        } else if (!sellAmountWrapper.isAmountSet()) {
            buyAmount = buyAmountWrapper.getAmount();
            sellAmount = buyAmount.convertedTo(sellUnit, CurrencyRatesProvider.reverseRate(actualRate), RoundingMode.HALF_DOWN);
        } else {
            buyAmount = buyAmountWrapper.getAmount();
            sellAmount = sellAmountWrapper.getAmount();
        }

        final BigDecimal naturalRate = calculateNaturalRate(buyUnit, sellUnit);
        if (naturalRate == null) {
            // we don't have rates in question for today yet, conserve operation to commit later
            accounter.rememberPostponedExchange(buyAmount, sellUnit, customRateRef, timestampRef.get());
            return;
        }

        if (customRateRef.isPresent()) {
            // that will introduce exchange difference between money hypothetically exchanged by default rate and money exchanged by custom rate
            FundsMutator.registerExchangeDifference(this, sellAmount.convertedTo(buyUnit, naturalRate, RoundingMode.HALF_DOWN), buyAmount, MutationDirection.BENEFIT, 1);
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

    private BigDecimal calculateNaturalRate(CurrencyUnit buyUnit, CurrencyUnit sellUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(timestampRef.get()), buyUnit, sellUnit).orElse(null));
        }
        return calculatedNaturalRate;
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
