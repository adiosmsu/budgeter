package ru.adios.budgeter;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 6/13/15
 * Time: 6:18 AM
 *
 * @author Mikhail Kulikov
 */
public final class ExchangeCurrenciesElementCore implements FundsMutator, Submitter {

    public static final String FIELD_BUY_ACCOUNT = "buyAccount";
    public static final String FIELD_SELL_ACCOUNT = "sellAccount";
    public static final String FIELD_AGENT = "agent";
    public static final String FIELD_BUY_AMOUNT_UNIT = "buyAmountUnit";
    public static final String FIELD_BUY_AMOUNT = "buyAmount";
    public static final String FIELD_SELL_AMOUNT_UNIT = "sellAmountUnit";
    public static final String FIELD_SELL_AMOUNT = "sellAmount";
    public static final String FIELD_BUY_AMOUNT_DECIMAL = "buyAmountDecimal";
    public static final String FIELD_SELL_AMOUNT_DECIMAL = "sellAmountDecimal";

    private static final Logger logger = LoggerFactory.getLogger(ExchangeCurrenciesElementCore.class);


    private final Accounter accounter;
    private final Treasury treasury;
    private final CurrenciesExchangeService ratesService;

    private final MoneyPositiveWrapper buyAmountWrapper = new MoneyPositiveWrapper("exchange buy amount");
    private final MoneyPositiveWrapper sellAmountWrapper = new MoneyPositiveWrapper("exchange sell amount");

    private Optional<Treasury.BalanceAccount> buyAccountRef = Optional.empty();
    private Optional<Treasury.BalanceAccount> sellAccountRef = Optional.empty();

    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private Optional<OffsetDateTime> timestampRef = Optional.of(OffsetDateTime.now());
    private Optional<FundsMutationAgent> agentRef = Optional.empty();
    private BigDecimal calculatedNaturalRate;
    private boolean personalMoneyExchange = false;

    private boolean lockOn = false;
    private Result storedResult;

    public ExchangeCurrenciesElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.treasury = treasury;
        this.ratesService = ratesService;
    }

    public boolean setPostponedEvent(PostponedCurrencyExchangeEventRepository.PostponedExchange event, BigDecimal naturalRate) {
        if (lockOn) return false;
        setAgent(event.agent);
        setBuyAmountDecimal(event.toBuy);
        setBuyAccount(event.toBuyAccount);
        setSellAccount(event.sellAccount);
        setCustomRate(event.customRate.orElse(null));
        setNaturalRate(naturalRate);
        setTimestamp(event.timestamp);
        return true;
    }

    public void setPersonalMoneyExchange(boolean personalMoneyExchange) {
        if (lockOn) return;
        this.personalMoneyExchange = personalMoneyExchange;
    }

    public void setNaturalRate(BigDecimal naturalRate) {
        if (lockOn) return;
        this.naturalRateRef = Optional.ofNullable(naturalRate);
    }

    @Nullable
    public BigDecimal getNaturalRate() {
        return naturalRateRef.orElse(null);
    }

    public void setBuyAmount(int coins, int cents) {
        if (lockOn) return;
        buyAmountWrapper.setAmount(coins, cents);
    }

    public void setBuyAmount(Money buyAmount) {
        if (lockOn) return;
        buyAmountWrapper.setAmount(buyAmount);
    }

    public void setBuyAmountDecimal(BigDecimal buyAmount) {
        if (lockOn) return;
        buyAmountWrapper.setAmountDecimal(buyAmount);
    }

    public BigDecimal getBuyAmountDecimal() {
        return buyAmountWrapper.getAmountDecimal();
    }

    public BigDecimal getSellAmountDecimal() {
        return sellAmountWrapper.getAmountDecimal();
    }

    public void setBuyAmountUnit(String buyAmountUnitName) {
        if (lockOn) return;
        buyAmountWrapper.setAmountUnit(buyAmountUnitName);
    }

    public void setBuyAmountUnit(CurrencyUnit buyUnit) {
        if (lockOn) return;
        buyAmountWrapper.setAmountUnit(buyUnit);
    }

    @Nullable
    public CurrencyUnit getBuyAmountUnit() {
        return buyAmountWrapper.getAmountUnit();
    }

    @Nullable
    public CurrencyUnit getSellAmountUnit() {
        return sellAmountWrapper.getAmountUnit();
    }

    public void setSellAmountUnit(CurrencyUnit sellUnit) {
        if (lockOn) return;
        sellAmountWrapper.setAmountUnit(sellUnit);
    }

    public void setSellAmountUnit(String sellAmountUnitName) {
        if (lockOn) return;
        sellAmountWrapper.setAmountUnit(sellAmountUnitName);
    }

    public void setSellAmountDecimal(BigDecimal sellAmount) {
        if (lockOn) return;
        sellAmountWrapper.setAmountDecimal(sellAmount);
    }

    public void setSellAmount(int coins, int cents) {
        if (lockOn) return;
        sellAmountWrapper.setAmount(coins, cents);
    }

    public void setSellAmount(Money sellAmount) {
        if (lockOn) return;
        sellAmountWrapper.setAmount(sellAmount);
    }

    public void setBuyAccount(Treasury.BalanceAccount buyAccount) {
        if (lockOn) return;
        this.buyAccountRef = Optional.of(buyAccount);
        buyAmountWrapper.setAmountUnit(buyAccount.getUnit());
    }

    @Nullable
    public Treasury.BalanceAccount getBuyAccount() {
        return buyAccountRef.orElse(null);
    }

    @Nullable
    public Treasury.BalanceAccount getSellAccount() {
        return sellAccountRef.orElse(null);
    }

    public void setSellAccount(Treasury.BalanceAccount sellAccount) {
        if (lockOn) return;
        this.sellAccountRef = Optional.of(sellAccount);
        sellAmountWrapper.setAmountUnit(sellAccount.getUnit());
    }

    public void setCustomRate(BigDecimal customRate) {
        if (lockOn) return;
        this.customRateRef = Optional.ofNullable(customRate);
    }

    @Nullable
    public BigDecimal getCustomRate() {
        return customRateRef.orElse(null);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        if (lockOn) return;
        this.timestampRef = Optional.of(timestamp);
    }

    @Nullable
    public OffsetDateTime getTimestamp() {
        return timestampRef.orElse(null);
    }

    public void setAgent(FundsMutationAgent agent) {
        if (lockOn) return;
        agentRef = Optional.of(agent);
    }

    @Nullable
    public FundsMutationAgent getAgent() {
        return agentRef.orElse(null);
    }

    /**
     * Orientation is: [buy amount] = [sell amount] * rate.
     */
    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(buyAccountRef, FIELD_BUY_ACCOUNT)
                .addFieldErrorIfAbsent(sellAccountRef, FIELD_SELL_ACCOUNT)
                .addFieldErrorIfAbsent(agentRef, FIELD_AGENT);

        if (!buyAmountWrapper.isUnitSet()) {
            resultBuilder.addFieldError(FIELD_BUY_AMOUNT_UNIT)
                    .addFieldError(FIELD_BUY_AMOUNT);
        }
        if (!sellAmountWrapper.isUnitSet()) {
            resultBuilder.addFieldError(FIELD_SELL_AMOUNT_UNIT)
                    .addFieldError(FIELD_SELL_AMOUNT);
        }

        if (!buyAmountWrapper.isAmountSet() && !sellAmountWrapper.isInitiable()) {
            resultBuilder.addFieldError(FIELD_BUY_AMOUNT_DECIMAL)
                    .addFieldError(FIELD_BUY_AMOUNT);
        }
        if (!sellAmountWrapper.isAmountSet() && !buyAmountWrapper.isInitiable()) {
            resultBuilder.addFieldError(FIELD_SELL_AMOUNT_DECIMAL)
                    .addFieldError(FIELD_SELL_AMOUNT);
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        try {
            final FundsMutationAgent agent = agentRef.get();

            final CurrencyUnit buyUnit = buyAmountWrapper.getAmountUnit();
            final CurrencyUnit sellUnit = sellAmountWrapper.getAmountUnit();

            if (!customRateRef.isPresent() && buyAmountWrapper.isAmountSet() && sellAmountWrapper.isAmountSet()) {
                final Money buyAmountChecked = buyAmountWrapper.getAmount();
                if (buyAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_BUY_AMOUNT_DECIMAL).build();
                }
                final Money sellAmountChecked = sellAmountWrapper.getAmount();
                if (sellAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_SELL_AMOUNT_DECIMAL).build();
                }
                customRateRef = Optional.of(CurrencyRatesProvider.calculateRate(buyAmountChecked.getAmount(), sellAmountChecked.getAmount()));
            }

            final BigMoney buyAmount, sellAmount;
            final BigDecimal actualRate = customRateRef.orElseGet(() -> calculateNaturalRate(sellUnit, buyUnit));
            if (!buyAmountWrapper.isAmountSet()) {
                final Money sellAmountChecked = sellAmountWrapper.getAmount();
                if (sellAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_SELL_AMOUNT_DECIMAL).build();
                }
                sellAmount = sellAmountChecked.toBigMoney();
                checkNotNull(buyUnit);
                buyAmount = sellAmount.convertedTo(buyUnit, actualRate);
            } else if (!sellAmountWrapper.isAmountSet()) {
                final Money buyAmountChecked = buyAmountWrapper.getAmount();
                if (buyAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_BUY_AMOUNT_DECIMAL).build();
                }
                buyAmount = buyAmountChecked.toBigMoney();
                checkNotNull(sellUnit);
                sellAmount = buyAmount.convertedTo(sellUnit, CurrencyRatesProvider.reverseRate(actualRate));
            } else {
                final Money buyAmountChecked = buyAmountWrapper.getAmount();
                if (buyAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_BUY_AMOUNT_DECIMAL).build();
                }
                buyAmount = buyAmountChecked.toBigMoney();
                final Money sellAmountChecked = sellAmountWrapper.getAmount();
                if (sellAmountChecked.isZero()) {
                    return resultBuilder.addFieldError(FIELD_SELL_AMOUNT_DECIMAL).build();
                }
                sellAmount = sellAmountChecked.toBigMoney();
            }
            final Money buyAmountSmallMoney = buyAmount.toMoney(RoundingMode.HALF_DOWN);

            final Treasury.BalanceAccount boughtAccount = buyAccountRef.get();
            final Treasury.BalanceAccount soldAccount = sellAccountRef.get();

            final BigDecimal naturalRate = calculateNaturalRate(sellUnit, buyUnit);
            if (naturalRate == null) {
                // we don't have rates in question for today yet, conserve operation to commit later
                accounter.rememberPostponedExchange(buyAmountSmallMoney.getAmount(), boughtAccount, soldAccount, customRateRef, timestampRef.get(), agent);
                return Result.success(null);
            }

            if (customRateRef.isPresent()) {
                checkNotNull(buyUnit);
                // that will introduce exchange difference between money hypothetically exchanged by default rate and money exchanged by custom rate
                FundsMutator.registerExchangeDifference(
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
        } catch (RuntimeException ex) {
            logger.error("Error while performing exchange currencies business logic", ex);
            return resultBuilder
                    .setGeneralError("Error while performing exchange currencies business logic: " + ex.getMessage())
                    .build();
        }

        return Result.success(null);
    }

    @Nullable
    private BigDecimal calculateNaturalRate(CurrencyUnit sellUnit, CurrencyUnit buyUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> {
                if (sellUnit == null || buyUnit == null) {
                    return null;
                }
                return ratesService.getConversionMultiplier(new UtcDay(timestampRef.get()), sellUnit, buyUnit).orElse(null);
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

    @Override
    public void lock() {
        lockOn = true;
    }

    @Override
    public void unlock() {
        lockOn = false;
    }

    @Override
    public Result getStoredResult() {
        return storedResult;
    }

    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
