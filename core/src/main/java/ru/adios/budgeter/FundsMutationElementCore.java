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
import ru.adios.budgeter.api.data.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 6/13/15
 * Time: 3:00 AM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public final class FundsMutationElementCore implements MoneySettable, TimestampSettable, FundsMutator, Submitter<BalanceAccount> {

    public static final String FIELD_DIRECTION = "direction";
    public static final String FIELD_RELEVANT_BALANCE = "relevantBalance";
    public static final String FIELD_AGENT = "agent";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_AMOUNT_UNIT = "amountUnit";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_AMOUNT_DECIMAL = "amountDecimal";
    public static final String FIELD_PAYEE_AMOUNT = "payeeAmount";
    public static final String FIELD_PAID_MONEY = "paidMoney";
    public static final String FIELD_PAYEE_ACCOUNT_UNIT = "payee_account_unit";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_TIMESTAMP = "timestamp";

    private static final Logger logger = LoggerFactory.getLogger(FundsMutationElementCore.class);


    private final Accounter accounter;
    private final CurrenciesExchangeService ratesService;
    private final Treasury treasury;
    private final SubmitHelper<BalanceAccount> helper = new SubmitHelper<>(logger, "Error while performing funds mutation business logic");

    private final MoneyPositiveWrapper amountWrapper = new MoneyPositiveWrapper("funds mutation amount");
    private final MoneyPositiveWrapper payeeAccountMoneyWrapper = new MoneyPositiveWrapper("funds mutation paid amount");

    private Optional<MutationDirection> directionRef = Optional.of(MutationDirection.BENEFIT);
    private FundsMutationEvent.Builder eventBuilder = FundsMutationEvent.builder();
    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private boolean mutateFunds = true;

    private PayeeMoneySettable payeeMoneySettable;
    private boolean lockOn = false;
    private Result<BalanceAccount> storedResult;

    public FundsMutationElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.ratesService = ratesService;
        this.treasury = treasury;
    }

    public MoneySettable getPayeeMoneySettable() {
        if (payeeMoneySettable == null) {
            payeeMoneySettable = new PayeeMoneySettable();
        }
        return payeeMoneySettable;
    }

    public boolean setPostponedEvent(PostponedMutationEvent event, BigDecimal naturalRate) {
        if (lockOn) return false;
        setEvent(event.mutationEvent);
        setAmount(event.mutationEvent.amount);
        setRelevantBalance(event.mutationEvent.relevantBalance);
        setDirection(MutationDirection.forEvent(event.mutationEvent));
        setPayeeAccountUnit(event.conversionUnit);
        setCustomRate(event.customRate.orElse(null));
        setNaturalRate(naturalRate);
        setTimestamp(event.mutationEvent.timestamp);
        return true;
    }

    public Money getSubmittedMoney() {
        final MutationDirection direction = getDirection();
        //noinspection ConstantConditions
        if (payeeAccountMoneyWrapper.isUnitSet() && !payeeAccountMoneyWrapper.getAmountUnit().equals(amountWrapper.getAmountUnit())) {
            final Money amount = direction.getAppropriateMutationAmount(amountWrapper.getAmount(), payeeAccountMoneyWrapper.getAmount());
            return direction.amountToSet(amount).multipliedBy(getQuantity());
        }
        return direction.amountToSet(amountWrapper.getAmount()).multipliedBy(getQuantity());
    }

    public void setDirection(MutationDirection direction) {
        if (lockOn) return;
        this.directionRef = Optional.ofNullable(direction);
        final BalanceAccount relevantBalance = eventBuilder.getRelevantBalance();
        if (relevantBalance != null) {
            adjustUnitsUsingBalance(direction, relevantBalance);
        }
    }

    public MutationDirection getDirection() {
        return directionRef.get();
    }

    public void setEvent(FundsMutationEvent event) {
        if (lockOn) return;
        eventBuilder.setFundsMutationEvent(event);
    }

    public void setNaturalRate(BigDecimal naturalRate) {
        if (lockOn) return;
        this.naturalRateRef = Optional.ofNullable(naturalRate);
    }

    @Nullable
    public BigDecimal getNaturalRate() {
        return naturalRateRef.orElse(null);
    }

    @Override
    public Money getAmount() {
        return amountWrapper.getAmount();
    }

    @Override
    public void setAmount(Money amount) {
        if (lockOn) return;

        final CurrencyUnit oldUnit = amountWrapper.getAmountUnit();

        amountWrapper.setAmount(amount);

        if (amount != null) {
            processAmountUnitChange(amount.getCurrencyUnit(), oldUnit);
        } else {
            adjustRelevantBalance(null, MutationDirection.BENEFIT);
            emptyRateRefs();
        }
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        if (lockOn) return;
        amountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        if (lockOn) return;
        if (code == null) {
            setAmountUnit((CurrencyUnit) null);
        } else {
            setAmountUnit(CurrencyUnit.of(code));
        }
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        if (lockOn) return;

        final CurrencyUnit oldUnit = amountWrapper.getAmountUnit();

        amountWrapper.setAmountUnit(unit);

        processAmountUnitChange(unit, oldUnit);
    }

    private void processAmountUnitChange(CurrencyUnit unit, CurrencyUnit oldUnit) {
        if (unit == null || !unit.equals(oldUnit)) {
            emptyRateRefs();
        }

        adjustRelevantBalance(unit, MutationDirection.BENEFIT);
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return amountWrapper.getAmountDecimal();
    }

    @Nullable
    @Override
    public CurrencyUnit getAmountUnit() {
        return amountWrapper.getAmountUnit();
    }

    public void setRelevantBalance(BalanceAccount relevantBalance) {
        if (lockOn) return;
        eventBuilder.setRelevantBalance(relevantBalance);
        if (directionRef.isPresent()) {
            adjustUnitsUsingBalance(directionRef.orElse(null), relevantBalance);
        }
    }

    @Nullable
    public BalanceAccount getRelevantBalance() {
        return eventBuilder.getRelevantBalance();
    }

    public void setPayeeAccountUnit(String code) {
        if (lockOn) return;
        if (code == null) {
            setPayeeAccountUnit((CurrencyUnit) null);
        } else {
            setPayeeAccountUnit(CurrencyUnit.of(code));
        }
    }

    public void setPayeeAccountUnit(CurrencyUnit unit) {
        if (lockOn) return;

        final CurrencyUnit oldUnit = payeeAccountMoneyWrapper.getAmountUnit();

        payeeAccountMoneyWrapper.setAmountUnit(unit);

        processPayedUnitChange(unit, oldUnit);
    }

    @Nullable
    public CurrencyUnit getPayeeAccountUnit() {
        return payeeAccountMoneyWrapper.getAmountUnit();
    }

    public void setPaidMoney(Money money) {
        if (lockOn) return;

        final CurrencyUnit oldUnit = payeeAccountMoneyWrapper.getAmountUnit();

        payeeAccountMoneyWrapper.setAmount(money);

        if (money != null) {
            processPayedUnitChange(money.getCurrencyUnit(), oldUnit);
        } else {
            adjustRelevantBalance(null, MutationDirection.LOSS);
            emptyRateRefs();
        }
    }

    private void processPayedUnitChange(CurrencyUnit unit, CurrencyUnit oldUnit) {
        if (unit == null || !unit.equals(oldUnit)) {
            emptyRateRefs();
        }

        adjustRelevantBalance(unit, MutationDirection.LOSS);
    }

    public Money getPaidMoney() {
        return payeeAccountMoneyWrapper.getAmount();
    }

    public void setPayeeAmount(int coins, int cents) {
        if (lockOn) return;
        payeeAccountMoneyWrapper.setAmount(coins, cents);
    }

    public void setPayeeAmount(BigDecimal amount) {
        if (lockOn) return;
        payeeAccountMoneyWrapper.setAmountDecimal(amount);
    }

    public BigDecimal getPayeeAmount() {
        return payeeAccountMoneyWrapper.getAmountDecimal();
    }

    public void setCustomRate(BigDecimal customRate) {
        if (lockOn) return;
        this.customRateRef = Optional.ofNullable(customRate);
    }

    @Nullable
    public BigDecimal getCustomRate() {
        return customRateRef.orElse(null);
    }

    public void setQuantity(int quantity) {
        if (lockOn) return;
        eventBuilder.setQuantity(quantity);
    }

    public int getQuantity() {
        return eventBuilder.getQuantity();
    }

    @PotentiallyBlocking
    public boolean setSubject(String subjectName) {
        if (lockOn) return false;
        if (subjectName == null) {
            eventBuilder.setSubject(null);
            return true;
        }
        final Optional<FundsMutationSubject> byName = accounter.fundsMutationSubjectRepo().findByName(subjectName);
        if (byName.isPresent()) {
            eventBuilder.setSubject(byName.get());
            return true;
        }
        return false;
    }

    public void setSubject(FundsMutationSubject subject) {
        if (lockOn) return;
        eventBuilder.setSubject(subject);
    }

    @Nullable
    public FundsMutationSubject getSubject() {
        return eventBuilder.getSubject();
    }

    @Override
    public void setTimestamp(@Nullable OffsetDateTime timestamp) {
        if (lockOn) return;
        eventBuilder.setTimestamp(timestamp);
    }

    @Override
    @Nullable
    public OffsetDateTime getTimestamp() {
        return eventBuilder.getTimestamp();
    }

    public void setMutateFunds(boolean mutateFunds) {
        if (lockOn) return;
        this.mutateFunds = mutateFunds;
    }

    public boolean getMutateFunds() {
        return mutateFunds;
    }

    public void setAgent(FundsMutationAgent agent) {
        if (lockOn) return;
        eventBuilder.setAgent(agent);
    }

    @PotentiallyBlocking
    public void setAgentString(String agentStr) {
        if (lockOn) return;
        final FundsMutationAgentRepository repo = accounter.fundsMutationAgentRepo();
        final Optional<FundsMutationAgent> byName = repo.findByName(agentStr);
        eventBuilder.setAgent(byName.orElseGet(() -> {
            final FundsMutationAgent agent = FundsMutationAgent.builder().setName(agentStr).build();
            return repo.addAgent(agent);
        }));
    }

    @Nullable
    public FundsMutationAgent getAgent() {
        return eventBuilder.getAgent();
    }

    /**
     * Orientation is: [amount] = [paid amount] * rate.
     */
    @PotentiallyBlocking
    @Override
    public Result<BalanceAccount> submit() {
        final ResultBuilder<BalanceAccount> resultBuilder = new ResultBuilder<>();
        resultBuilder.addFieldErrorIfAbsent(directionRef, FIELD_DIRECTION)
                .addFieldErrorIfNull(eventBuilder.getRelevantBalance(), FIELD_RELEVANT_BALANCE)
                .addFieldErrorIfNull(eventBuilder.getAgent(), FIELD_AGENT)
                .addFieldErrorIfNull(eventBuilder.getSubject(), FIELD_SUBJECT)
                .addFieldErrorIfNull(eventBuilder.getTimestamp(), FIELD_TIMESTAMP)
                .addFieldErrorIfNegative(amountWrapper, FIELD_AMOUNT_DECIMAL)
                .addFieldErrorIfNegative(payeeAccountMoneyWrapper, FIELD_PAYEE_AMOUNT);

        if (!amountWrapper.isUnitSet()) {
            resultBuilder.addFieldError(FIELD_AMOUNT_UNIT)
                    .addFieldError(FIELD_AMOUNT);
        }

        if (payeeAccountMoneyWrapper.isUnitSet() && !amountWrapper.isAmountSet() && !payeeAccountMoneyWrapper.isAmountSet()) {
            resultBuilder.addFieldError(FIELD_AMOUNT)
                    .addFieldError(FIELD_AMOUNT_DECIMAL)
                    .addFieldError(FIELD_PAYEE_AMOUNT)
                    .addFieldError(FIELD_PAID_MONEY);
        }

        if (!payeeAccountMoneyWrapper.isUnitSet() && payeeAccountMoneyWrapper.isAmountSet()) {
            resultBuilder.addFieldError(FIELD_PAYEE_ACCOUNT_UNIT);
        }

        if (eventBuilder.getQuantity() <= 0) {
            resultBuilder.addFieldError(FIELD_QUANTITY, Submitter.ResultBuilder.POSITIVE_PRE);
        }

        if (customRateRef.isPresent() && payeeAccountMoneyWrapper.isInitiable() && amountWrapper.isInitiable()) {
            final BigDecimal rate = CurrencyRatesProvider.calculateRate(amountWrapper.getAmountDecimal(), payeeAccountMoneyWrapper.getAmountDecimal());
            if (!customRateRef.get().setScale(6, BigDecimal.ROUND_HALF_DOWN).equals(rate.setScale(6, BigDecimal.ROUND_HALF_DOWN))) {
                resultBuilder.addFieldError(FIELD_AMOUNT_DECIMAL, "Custom rate is different from rate of %s and", new Object[] {FIELD_PAYEE_AMOUNT});
                resultBuilder.addFieldError(FIELD_PAYEE_AMOUNT, "Custom rate is different from rate of %s and", new Object[] {FIELD_AMOUNT_DECIMAL});
            }
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        return helper.doSubmit(this::doSubmit, resultBuilder);
    }

    @Nonnull
    private Result<BalanceAccount> doSubmit() {
        final MutationDirection direction = directionRef.get();
        final CurrencyUnit amountUnit = amountWrapper.getAmountUnit();

        final BigMoney amount;
        if (payeeAccountMoneyWrapper.isUnitSet()) {
            // and so actual account of payment was set (even if only as a currency)
            final CurrencyUnit paidUnit = payeeAccountMoneyWrapper.getAmountUnit(); // therefore we know it for sure
            checkNotNull(paidUnit);
            final boolean sameUnits = paidUnit.equals(amountUnit);

            if (!customRateRef.isPresent() && payeeAccountMoneyWrapper.isAmountSet() && amountWrapper.isAmountSet()) {
                // we know about paid money and actual amount both therefore we must calculate custom rate which with 99% chance will differ from natural rate
                customRateRef = Optional.of(CurrencyRatesProvider.calculateRate(amountWrapper.getAmount().getAmount(), payeeAccountMoneyWrapper.getAmount().getAmount()));
            }

            if (!amountWrapper.isAmountSet()) {
                // actual amount wasn't set explicitly so we must calculate it from paid amount and rate (custom or natural) which must have been provided
                final BigMoney paidAmount = payeeAccountMoneyWrapper.getAmount().toBigMoney();
                checkNotNull(amountUnit);
                if (sameUnits) {
                    amount = paidAmount;
                    if (!amountWrapper.isInitiable()) {
                        amountWrapper.setAmount(paidAmount.toMoney(RoundingMode.HALF_DOWN)); // set discovered money
                    }
                } else {
                    amount = paidAmount.convertedTo(amountUnit, customRateRef.orElseGet(() -> calculateNaturalRate(paidUnit, amountUnit)));
                }
            } else {
                // actual amount was set explicitly
                amount = amountWrapper.getAmount().toBigMoney();
            }

            if (!sameUnits) {
                // currency conversion to be
                final BigDecimal naturalRate = calculateNaturalRate(paidUnit, amountUnit);
                final Money amountSmallMoney = amount.toMoney(RoundingMode.HALF_DOWN);
                if (!amountWrapper.isInitiable()) {
                    amountWrapper.setAmount(amountSmallMoney); // set discovered money
                }
                if (naturalRate == null) {
                    // we don't have today's rates yet, do accounting later
                    direction.remember(accounter, eventBuilder.setAmount(amountSmallMoney).build(), paidUnit, customRateRef);
                    return Result.success(null);
                }

                final BigMoney soldAmount;
                final BigDecimal actualRate = customRateRef.orElse(naturalRate);
                if (payeeAccountMoneyWrapper.isAmountSet()) {
                    soldAmount = payeeAccountMoneyWrapper.getAmount().toBigMoney();
                } else {
                    soldAmount = amount.convertedTo(paidUnit, CurrencyRatesProvider.reverseRate(actualRate));
                }

                final Money soldAmountSmallMoney = soldAmount.toMoney(RoundingMode.HALF_DOWN);
                if (!payeeAccountMoneyWrapper.isInitiable()) {
                    payeeAccountMoneyWrapper.setAmount(soldAmountSmallMoney); // set discovered money paid
                }
                final Money appropriateMutationAmount = direction.getAppropriateMutationAmount(amountSmallMoney, soldAmountSmallMoney);

                if (customRateRef.isPresent()) {
                    // custom exchange rate present, will need to calculate difference and account it
                    final BigMoney naturalAmount;
                    final BigMoney convertedAmount;
                    if (appropriateMutationAmount.getCurrencyUnit().equals(amountUnit)) {
                        naturalAmount = soldAmount.convertedTo(amountUnit, naturalRate);
                        convertedAmount = amount;
                    } else {
                        naturalAmount = amount.convertedTo(paidUnit, CurrencyRatesProvider.reverseRate(naturalRate));
                        convertedAmount = soldAmount;
                    }

                    FundsMutator.registerExchangeDifference(
                            this,
                            naturalAmount.toMoney(RoundingMode.HALF_DOWN),
                            convertedAmount.toMoney(RoundingMode.HALF_DOWN),
                            eventBuilder.getRelevantBalance(),
                            direction,
                            eventBuilder.getAgent(),
                            eventBuilder.getTimestamp(),
                            eventBuilder.getQuantity()
                    );
                }

                final BalanceAccount res = direction.register(accounter, treasury, eventBuilder, appropriateMutationAmount, mutateFunds);
                accounter.currencyExchangeEventRepository().registerCurrencyExchange(
                        CurrencyExchangeEvent.builder()
                                .setAgent(eventBuilder.getAgent())
                                .setRate(actualRate)
                                .setBought(amountSmallMoney)
                                .setSold(soldAmountSmallMoney)
                                .setBoughtAccount(direction == MutationDirection.LOSS
                                        ? Treasury.getTransitoryAccount(amountSmallMoney.getCurrencyUnit(), treasury)
                                        : eventBuilder.getRelevantBalance())
                                .setSoldAccount(direction == MutationDirection.LOSS
                                        ? eventBuilder.getRelevantBalance()
                                        : Treasury.getTransitoryAccount(soldAmountSmallMoney.getCurrencyUnit(), treasury))
                                .setTimestamp(eventBuilder.getTimestamp())
                                .build()
                );
                return Result.success(res);
            }
        } else {
            amount = amountWrapper.getAmount().toBigMoney();
        }

        return Result.success(direction.register(accounter, treasury, eventBuilder, amount.toMoney(), mutateFunds));
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
    private BigDecimal calculateNaturalRate(CurrencyUnit paidUnit, CurrencyUnit amountUnit) {
        if (!naturalRateRef.isPresent()) {
            naturalRateRef = ratesService.getConversionMultiplier(new UtcDay(eventBuilder.getTimestamp()), paidUnit, amountUnit);
        }
        return naturalRateRef.orElse(null);
    }

    @Override
    public Accounter getAccounter() {
        return accounter;
    }

    @Override
    public Treasury getTreasury() {
        return treasury;
    }

    @Override
    public CurrenciesExchangeService getRatesService() {
        return ratesService;
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
    public Result<BalanceAccount> getStoredResult() {
        return storedResult;
    }

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

    private void adjustUnitsUsingBalance(@Nullable MutationDirection dir, @Nullable BalanceAccount relevantBalance) {
        if (relevantBalance != null && dir != null) {
            final CurrencyUnit relevantBalanceUnit = relevantBalance.getUnit();
            switch (dir) {
                case LOSS:
                    setPayeeAccountUnit(relevantBalanceUnit);
                    break;
                case BENEFIT:
                    setAmountUnit(relevantBalanceUnit);
            }
        }
    }

    private void adjustRelevantBalance(@Nullable CurrencyUnit unit, MutationDirection dir) {
        final BalanceAccount relevantBalance = getRelevantBalance();
        if (directionRef.isPresent() && relevantBalance != null && directionRef.get() == dir && !relevantBalance.getUnit().equals(unit)) {
            eventBuilder.setRelevantBalance(null);
        }
    }

    private void emptyRateRefs() {
        naturalRateRef = Optional.empty();
        customRateRef = Optional.empty();
    }

    public final class PayeeMoneySettable implements MoneySettable {

        private PayeeMoneySettable() {}

        @Override
        public void setAmount(int coins, int cents) {
            setPayeeAmount(coins, cents);
        }

        @Override
        public void setAmountDecimal(BigDecimal amountDecimal) {
            setPayeeAmount(amountDecimal);
        }

        @Override
        public BigDecimal getAmountDecimal() {
            return getPayeeAmount();
        }

        @Override
        public void setAmountUnit(String code) {
            setPayeeAccountUnit(code);
        }

        @Override
        public void setAmountUnit(CurrencyUnit unit) {
            setPayeeAccountUnit(unit);
        }

        @Override
        public CurrencyUnit getAmountUnit() {
            return getPayeeAccountUnit();
        }

        @Override
        public void setAmount(Money amount) {
            setPaidMoney(amount);
        }

        @Override
        public Money getAmount() {
            return getPaidMoney();
        }

    }

}
