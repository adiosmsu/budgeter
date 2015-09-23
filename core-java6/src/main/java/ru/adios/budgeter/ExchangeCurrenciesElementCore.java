package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Supplier;
import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    private Optional<Treasury.BalanceAccount> buyAccountRef = Optional.empty();
    private Optional<Treasury.BalanceAccount> sellAccountRef = Optional.empty();

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
        setBuyAmountDecimal(event.toBuy);
        setBuyAccount(event.toBuyAccount);
        setSellAccount(event.sellAccount);
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

    public BigDecimal getNaturalRate() {
        return naturalRateRef.orElse(null);
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

    public BigDecimal getBuyAmountDecimal() {
        return buyAmountWrapper.getAmountDecimal();
    }

    public BigDecimal getSellAmountDecimal() {
        return sellAmountWrapper.getAmountDecimal();
    }

    public void setBuyAmountUnit(String buyAmountUnitName) {
        buyAmountWrapper.setAmountUnit(buyAmountUnitName);
    }

    public void setBuyAmountUnit(CurrencyUnit buyUnit) {
        buyAmountWrapper.setAmountUnit(buyUnit);
    }

    public CurrencyUnit getBuyAmountUnit() {
        return buyAmountWrapper.getAmountUnit();
    }

    public CurrencyUnit getSellAmountUnit() {
        return sellAmountWrapper.getAmountUnit();
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

    public void setBuyAccount(Treasury.BalanceAccount buyAccount) {
        this.buyAccountRef = Optional.of(buyAccount);
        buyAmountWrapper.setAmountUnit(buyAccount.getUnit());
    }

    public Treasury.BalanceAccount getBuyAccount() {
        return buyAccountRef.orElse(null);
    }

    public Treasury.BalanceAccount getSellAccount() {
        return sellAccountRef.orElse(null);
    }

    public void setSellAccount(Treasury.BalanceAccount sellAccount) {
        this.sellAccountRef = Optional.of(sellAccount);
        sellAmountWrapper.setAmountUnit(sellAccount.getUnit());
    }

    public void setCustomRate(BigDecimal customRate) {
        this.customRateRef = Optional.ofNullable(customRate);
    }

    public BigDecimal getCustomRate() {
        return customRateRef.orElse(null);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestampRef = Optional.of(timestamp);
    }

    public OffsetDateTime getTimestamp() {
        return timestampRef.orElse(null);
    }

    public void setAgent(FundsMutationAgent agent) {
        agentRef = Optional.of(agent);
    }

    public FundsMutationAgent getAgent() {
        return agentRef.orElse(null);
    }

    /**
     * Orientation is: [buy amount] = [sell amount] * rate.
     */
    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(buyAccountRef, "buyAccount")
                .addFieldErrorIfAbsent(sellAccountRef, "sellAccount")
                .addFieldErrorIfAbsent(agentRef, "agent");

        if (!buyAmountWrapper.isUnitSet()) {
            resultBuilder.addFieldError("buyAmountUnit")
                    .addFieldError("buyAmount");
        }
        if (!sellAmountWrapper.isUnitSet()) {
            resultBuilder.addFieldError("sellAmountUnit")
                    .addFieldError("sellAmount");
        }

        if (!buyAmountWrapper.isAmountSet() && !sellAmountWrapper.isInitiable()) {
            resultBuilder.addFieldError("buyAmountDecimal")
                    .addFieldError("buyAmount");
        }
        if (!sellAmountWrapper.isAmountSet() && !buyAmountWrapper.isInitiable()) {
            resultBuilder.addFieldError("sellAmountDecimal")
                    .addFieldError("sellAmount");
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        final FundsMutationAgent agent = agentRef.get();

        final CurrencyUnit buyUnit = buyAmountWrapper.getAmountUnit();
        final CurrencyUnit sellUnit = sellAmountWrapper.getAmountUnit();

        if (!customRateRef.isPresent() && buyAmountWrapper.isAmountSet() && sellAmountWrapper.isAmountSet()) {
            customRateRef = Optional.of(CurrencyRatesProvider.Static.calculateRate(buyAmountWrapper.getAmount().getAmount(), sellAmountWrapper.getAmount().getAmount()));
        }

        final BigMoney buyAmount, sellAmount;
        final BigDecimal actualRate = customRateRef.orElseGet(new Supplier<BigDecimal>() {
            @Override
            public BigDecimal get() {
                return calculateNaturalRate(sellUnit, buyUnit);
            }
        });
        if (!buyAmountWrapper.isAmountSet()) {
            sellAmount = sellAmountWrapper.getAmount().toBigMoney();
            buyAmount = sellAmount.convertedTo(buyUnit, actualRate);
        } else if (!sellAmountWrapper.isAmountSet()) {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            sellAmount = buyAmount.convertedTo(sellUnit, CurrencyRatesProvider.Static.reverseRate(actualRate));
        } else {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            sellAmount = sellAmountWrapper.getAmount().toBigMoney();
        }
        final Money buyAmountSmallMoney = buyAmount.toMoney(RoundingMode.HALF_DOWN);

        final Treasury.BalanceAccount boughtAccount = buyAccountRef.get();
        final Treasury.BalanceAccount soldAccount = sellAccountRef.get();

        final BigDecimal naturalRate = calculateNaturalRate(sellUnit, buyUnit);
        if (naturalRate == null) {
            // we don't have rates in question for today yet, conserve operation to commit later
            accounter.rememberPostponedExchange(buyAmountSmallMoney.getAmount(), boughtAccount, soldAccount, customRateRef, timestampRef.get(), agent);
            return Result.SUCCESS;
        }

        if (customRateRef.isPresent()) {
            // that will introduce exchange difference between money hypothetically exchanged by default rate and money exchanged by custom rate
            FundsMutator.Static.registerExchangeDifference(
                    this,
                    sellAmount.convertedTo(buyUnit, naturalRate).toMoney(RoundingMode.HALF_DOWN),
                    buyAmountSmallMoney,
                    boughtAccount,
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
                        .setBoughtAccount(boughtAccount)
                        .setSoldAccount(soldAccount)
                        .setRate(actualRate)
                        .setTimestamp(timestampRef.get())
                        .setAgent(agent)
                        .build()
        );

        if (personalMoneyExchange) {
            treasury.addAmount(buyAmountSmallMoney, boughtAccount.name);
            treasury.addAmount(sellAmountSmallMoney.negated(), soldAccount.name);
        }

        return Result.SUCCESS;
    }

    private BigDecimal calculateNaturalRate(final CurrencyUnit sellUnit, final CurrencyUnit buyUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(new Supplier<BigDecimal>() {
                @Override
                public BigDecimal get() {
                    if (sellUnit == null || buyUnit == null) {
                        return null;
                    }
                    return ratesService.getConversionMultiplier(new UtcDay(timestampRef.get()), sellUnit, buyUnit).orElse(null);
                }
            });
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
