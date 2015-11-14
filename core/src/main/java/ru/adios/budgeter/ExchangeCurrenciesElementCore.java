/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.PostponedExchange;

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
public final class ExchangeCurrenciesElementCore implements FundsMutator, Submitter, TimestampSettable {

    public static final String FIELD_BUY_ACCOUNT = "buyAccount";
    public static final String FIELD_SELL_ACCOUNT = "sellAccount";
    public static final String FIELD_AGENT = "agent";
    public static final String FIELD_BUY_AMOUNT_UNIT = "buyAmountUnit";
    public static final String FIELD_BUY_AMOUNT = "buyAmount";
    public static final String FIELD_SELL_AMOUNT_UNIT = "sellAmountUnit";
    public static final String FIELD_SELL_AMOUNT = "sellAmount";
    public static final String FIELD_BUY_AMOUNT_DECIMAL = "buyAmountDecimal";
    public static final String FIELD_SELL_AMOUNT_DECIMAL = "sellAmountDecimal";
    public static final String FIELD_TIMESTAMP = "timestamp";

    private static final Logger logger = LoggerFactory.getLogger(ExchangeCurrenciesElementCore.class);


    private final Accounter accounter;
    private final Treasury treasury;
    private final CurrenciesExchangeService ratesService;
    private final SubmitHelper helper = new SubmitHelper(logger, "Error while performing exchange currencies business logic");

    private final MoneyPositiveWrapper buyAmountWrapper = new MoneyPositiveWrapper("exchange buy amount");
    private final MoneyPositiveWrapper sellAmountWrapper = new MoneyPositiveWrapper("exchange sell amount");

    private Optional<BalanceAccount> buyAccountRef = Optional.empty();
    private Optional<BalanceAccount> sellAccountRef = Optional.empty();

    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private Optional<OffsetDateTime> timestampRef = Optional.of(OffsetDateTime.now());
    private Optional<FundsMutationAgent> agentRef = Optional.empty();
    private BigDecimal calculatedNaturalRate;
    private boolean personalMoneyExchange = false;

    private BuyMoneySettable buyMoneySettable;
    private SellMoneySettable sellMoneySettable;
    private boolean lockOn = false;
    private Result storedResult;

    public ExchangeCurrenciesElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.treasury = treasury;
        this.ratesService = ratesService;
    }

    public MoneySettable getBuyMoneySettable() {
        if (buyMoneySettable == null) {
            buyMoneySettable = new BuyMoneySettable();
        }
        return buyMoneySettable;
    }

    public MoneySettable getSellMoneySettable() {
        if (sellMoneySettable == null) {
            sellMoneySettable = new SellMoneySettable();
        }
        return sellMoneySettable;
    }

    public boolean setPostponedEvent(PostponedExchange event, BigDecimal naturalRate) {
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

    public void setBuyAccount(BalanceAccount buyAccount) {
        if (lockOn) return;
        this.buyAccountRef = Optional.ofNullable(buyAccount);
        if (buyAccount != null) {
            buyAmountWrapper.setAmountUnit(buyAccount.getUnit());
        } else {
            buyAmountWrapper.setAmountUnit((CurrencyUnit) null);
        }
    }

    @Nullable
    public BalanceAccount getBuyAccount() {
        return buyAccountRef.orElse(null);
    }

    @Nullable
    public BalanceAccount getSellAccount() {
        return sellAccountRef.orElse(null);
    }

    public void setSellAccount(BalanceAccount sellAccount) {
        if (lockOn) return;
        this.sellAccountRef = Optional.ofNullable(sellAccount);
        if (sellAccount != null) {
            sellAmountWrapper.setAmountUnit(sellAccount.getUnit());
        } else {
            sellAmountWrapper.setAmountUnit((CurrencyUnit) null);
        }
    }

    public void setCustomRate(BigDecimal customRate) {
        if (lockOn) return;
        this.customRateRef = Optional.ofNullable(customRate);
    }

    @Nullable
    public BigDecimal getCustomRate() {
        return customRateRef.orElse(null);
    }

    @Override
    public void setTimestamp(@Nullable OffsetDateTime timestamp) {
        if (lockOn) return;
        this.timestampRef = Optional.ofNullable(timestamp);
    }

    @Nullable
    @Override
    public OffsetDateTime getTimestamp() {
        return timestampRef.orElse(null);
    }

    public void setAgent(FundsMutationAgent agent) {
        if (lockOn) return;
        agentRef = Optional.ofNullable(agent);
    }

    @Nullable
    public FundsMutationAgent getAgent() {
        return agentRef.orElse(null);
    }

    /**
     * Orientation is: [buy amount] = [sell amount] * rate.
     */
    @PotentiallyBlocking
    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(buyAccountRef, FIELD_BUY_ACCOUNT)
                .addFieldErrorIfAbsent(sellAccountRef, FIELD_SELL_ACCOUNT)
                .addFieldErrorIfAbsent(timestampRef, FIELD_TIMESTAMP)
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

        if (customRateRef.isPresent() && buyAmountWrapper.isInitiable() && sellAmountWrapper.isInitiable()) {
            final BigDecimal rate = CurrencyRatesProvider.calculateRate(buyAmountWrapper.getAmountDecimal(), sellAmountWrapper.getAmountDecimal());
            if (!customRateRef.get().setScale(6, BigDecimal.ROUND_HALF_DOWN).equals(rate.setScale(6, BigDecimal.ROUND_HALF_DOWN))) {
                resultBuilder.addFieldError(FIELD_BUY_AMOUNT_DECIMAL, "Custom rate is different from rate of %s and", new Object[] {FIELD_BUY_AMOUNT_DECIMAL});
                resultBuilder.addFieldError(FIELD_SELL_AMOUNT_DECIMAL, "Custom rate is different from rate of %s and", new Object[] {FIELD_BUY_AMOUNT_DECIMAL});
            }
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        //noinspection unchecked
        return helper.doSubmit(this::doSubmit, resultBuilder);
    }

    private Result doSubmit() {
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
            checkNotNull(buyUnit);
            buyAmount = sellAmount.convertedTo(buyUnit, actualRate);
        } else if (!sellAmountWrapper.isAmountSet()) {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            checkNotNull(sellUnit);
            sellAmount = buyAmount.convertedTo(sellUnit, CurrencyRatesProvider.reverseRate(actualRate));
        } else {
            buyAmount = buyAmountWrapper.getAmount().toBigMoney();
            sellAmount = sellAmountWrapper.getAmount().toBigMoney();
        }
        final Money buyAmountSmallMoney = buyAmount.toMoney(RoundingMode.HALF_DOWN);

        final BalanceAccount boughtAccount = buyAccountRef.get();
        final BalanceAccount soldAccount = sellAccountRef.get();

        final BigDecimal naturalRate = calculateNaturalRate(sellUnit, buyUnit);
        if (naturalRate == null) {
            // we don't have rates in question for today yet, conserve operation to commit later
            accounter.postponedCurrencyExchangeEventRepository()
                    .rememberPostponedExchange(buyAmountSmallMoney.getAmount(), boughtAccount, soldAccount, customRateRef, timestampRef.get(), agent);
            return Result.success(null);
        } else {
            if (!naturalRateRef.isPresent()) {
                naturalRateRef = Optional.of(naturalRate);
            }
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
                    BigDecimal.ONE
            );
        }

        final Money sellAmountSmallMoney = sellAmount.toMoney(RoundingMode.HALF_DOWN);
        accounter.currencyExchangeEventRepository().registerCurrencyExchange(
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

        if (!buyAmountWrapper.isInitiable()) {
            buyAmountWrapper.setAmount(buyAmountSmallMoney);
        }
        if (!sellAmountWrapper.isInitiable()) {
            sellAmountWrapper.setAmount(sellAmountSmallMoney);
        }

        if (personalMoneyExchange) {
            treasury.addAmount(buyAmountSmallMoney, boughtAccount.name);
            treasury.addAmount(sellAmountSmallMoney.negated(), soldAccount.name);
            buyAccountRef = treasury.getAccountForName(boughtAccount.name);
            sellAccountRef = treasury.getAccountForName(soldAccount.name);
        }

        return Result.success(null);
    }

    @Override
    public void setTransactional(TransactionalSupport transactional) {
        helper.setTransactionalSupport(transactional);
    }

    @Override
    public TransactionalSupport getTransactional() {
        return helper.getTransactionalSupport();
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

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

    public final class BuyMoneySettable implements MoneySettable {

        private BuyMoneySettable() {}

        @Override
        public Money getAmount() {
            return buyAmountWrapper.getAmount();
        }

        @Override
        public void setAmount(int coins, int cents) {
            setBuyAmount(coins, cents);
        }

        @Override
        public void setAmountDecimal(BigDecimal amountDecimal) {
            setBuyAmountDecimal(amountDecimal);
        }

        @Override
        public BigDecimal getAmountDecimal() {
            return getBuyAmountDecimal();
        }

        @Override
        public void setAmountUnit(String code) {
            setBuyAmountUnit(code);
        }

        @Override
        public void setAmountUnit(CurrencyUnit unit) {
            setBuyAmountUnit(unit);
        }

        @Override
        public CurrencyUnit getAmountUnit() {
            return getBuyAmountUnit();
        }

        @Override
        public void setAmount(Money amount) {
            setBuyAmount(amount);
        }

    }

    public final class SellMoneySettable implements MoneySettable {

        private SellMoneySettable() {}

        @Override
        public Money getAmount() {
            return sellAmountWrapper.getAmount();
        }

        @Override
        public void setAmount(int coins, int cents) {
            setSellAmount(coins, cents);
        }

        @Override
        public void setAmountDecimal(BigDecimal amountDecimal) {
            setSellAmountDecimal(amountDecimal);
        }

        @Override
        public BigDecimal getAmountDecimal() {
            return getSellAmountDecimal();
        }

        @Override
        public void setAmountUnit(String code) {
            setSellAmountUnit(code);
        }

        @Override
        public void setAmountUnit(CurrencyUnit unit) {
            setSellAmountUnit(unit);
        }

        @Override
        public CurrencyUnit getAmountUnit() {
            return getSellAmountUnit();
        }

        @Override
        public void setAmount(Money amount) {
            setSellAmount(amount);
        }

    }

}
