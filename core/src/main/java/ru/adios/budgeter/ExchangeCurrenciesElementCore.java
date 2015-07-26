package ru.adios.budgeter;

import com.google.common.base.Preconditions;
import org.joda.money.BigMoney;
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
    private Optional<FundsMutationAgent> agentRef = Optional.empty();
    private BigDecimal calculatedNaturalRate;
    private boolean personalMoneyExchange = false;

    public ExchangeCurrenciesElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.treasury = treasury;
        this.ratesService = ratesService;
    }

    public void setPostponedEvent(PostponedCurrencyExchangeEventRepository.PostponedExchange event, BigDecimal naturalRate) {
        setAgent(event.agent);
        setBuyAmount(event.toBuy);
        setSellAmountUnit(event.unitSell);
        setCustomRate(event.customRate.orElse(null));
        setNaturalRate(naturalRate);
        setTimestamp(event.timestamp);
    }

    public void setPersonalMoneyExchange(boolean personalMoneyExchange) {
        this.personalMoneyExchange = personalMoneyExchange;
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

    public void setBuyAmountUnit(CurrencyUnit buyUnit) {
        buyAmountWrapper.setAmountUnit(buyUnit);
    }

    public void setSellAmountUnit(CurrencyUnit sellUnit) {
        sellAmountWrapper.setAmountUnit(sellUnit);
    }

    public void setSellAmountUnit(String sellAmountUnitName) {
        sellAmountWrapper.setAmountUnit(sellAmountUnitName);
    }

    public void setSellAmountDecimal(BigDecimal sellAmount) {
        sellAmountWrapper.setAmountDecimal(sellAmount);
    }

    public void setSellAmount(int coins, int cents) {
        sellAmountWrapper.setAmount(coins, cents);
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

    public void setAgent(FundsMutationAgent agent) {
        agentRef = Optional.of(agent);
    }

    /**
     * Orientation is: [buy amount] = [sell amount] * rate.
     */
    @Override
    public void submit() {
        Preconditions.checkState(agentRef.isPresent(), "Agent not set");
        final FundsMutationAgent agent = agentRef.get();

        final CurrencyUnit buyUnit = buyAmountWrapper.getAmountUnit();
        final CurrencyUnit sellUnit = sellAmountWrapper.getAmountUnit();

        if (!customRateRef.isPresent() && buyAmountWrapper.isAmountSet() && sellAmountWrapper.isAmountSet()) {
            customRateRef = Optional.of(CurrencyRatesProvider.calculateRate(buyAmountWrapper.getAmount().getAmount(), sellAmountWrapper.getAmount().getAmount()));
        }

        final BigMoney buyAmount, sellAmount;
        final BigDecimal actualRate = customRateRef.orElseGet(() -> calculateNaturalRate(sellUnit, buyUnit));
        if (!buyAmountWrapper.isAmountSet()) {
            sellAmount = sellAmountWrapper.getAmount().toBigMoney();
            buyAmount = sellAmount.convertedTo(buyUnit, actualRate);
        } else if (!sellAmountWrapper.isAmountSet()) {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            sellAmount = buyAmount.convertedTo(sellUnit, CurrencyRatesProvider.reverseRate(actualRate));
        } else {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            sellAmount = sellAmountWrapper.getAmount().toBigMoney();
        }
        final Money buyAmountSmallMoney = buyAmount.toMoney(RoundingMode.HALF_DOWN);

        final BigDecimal naturalRate = calculateNaturalRate(sellUnit, buyUnit);
        if (naturalRate == null) {
            // we don't have rates in question for today yet, conserve operation to commit later
            accounter.rememberPostponedExchange(buyAmountSmallMoney, sellUnit, customRateRef, timestampRef.get(), agent);
            return;
        }

        if (customRateRef.isPresent()) {
            // that will introduce exchange difference between money hypothetically exchanged by default rate and money exchanged by custom rate
            FundsMutator.registerExchangeDifference(
                    this,
                    sellAmount.convertedTo(buyUnit, naturalRate).toMoney(RoundingMode.HALF_DOWN),
                    buyAmountSmallMoney,
                    MutationDirection.BENEFIT,
                    agent,
                    timestampRef.get(),
                    1
            );
        }

        final Money sellAmountSmallMoney = sellAmount.toMoney(RoundingMode.HALF_DOWN);
        accounter.registerCurrencyExchange(
                CurrencyExchangeEvent.builder()
                        .setBought(buyAmountSmallMoney)
                        .setSold(sellAmountSmallMoney)
                        .setRate(actualRate)
                        .setTimestamp(timestampRef.get())
                        .setAgent(agent)
                        .build()
        );

        if (personalMoneyExchange) {
            treasury.addAmount(buyAmountSmallMoney);
            treasury.addAmount(sellAmountSmallMoney.negated());
        }
    }

    private BigDecimal calculateNaturalRate(CurrencyUnit sellUnit, CurrencyUnit buyUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(timestampRef.get()), sellUnit, buyUnit).orElse(null));
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
