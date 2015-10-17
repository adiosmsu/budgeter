package ru.adios.budgeter;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.*;

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
public final class FundsMutationElementCore implements MoneySettable, FundsMutator, Submitter {

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

    private final MoneyWrapperBean amountWrapper = new MoneyWrapperBean("funds mutation amount");
    private final MoneyWrapperBean payeeAccountMoneyWrapper = new MoneyWrapperBean("funds mutation paid amount");

    private Optional<MutationDirection> directionRef = Optional.of(MutationDirection.BENEFIT);
    private FundsMutationEvent.Builder eventBuilder = FundsMutationEvent.builder();
    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();
    private boolean mutateFunds = true;
    private BigDecimal calculatedNaturalRate;

    public FundsMutationElementCore(Accounter accounter, Treasury treasury, CurrenciesExchangeService ratesService) {
        this.accounter = accounter;
        this.ratesService = ratesService;
        this.treasury = treasury;
    }

    public void setPostponedEvent(PostponedFundsMutationEventRepository.PostponedMutationEvent event, BigDecimal naturalRate) {
        setEvent(event.mutationEvent);
        setAmount(event.mutationEvent.amount);
        setRelevantBalance(event.mutationEvent.relevantBalance);
        setDirection(MutationDirection.forEvent(event.mutationEvent));
        setPayeeAccountUnit(event.conversionUnit);
        setCustomRate(event.customRate.orElse(null));
        setNaturalRate(naturalRate);
        setTimestamp(event.mutationEvent.timestamp);
    }

    public void setDirection(MutationDirection direction) {
        this.directionRef = Optional.of(direction);
        final Treasury.BalanceAccount relevantBalance = eventBuilder.getRelevantBalance();
        if (relevantBalance != null) {
            adjustUnitsUsingBalance(direction, relevantBalance);
        }
    }

    public MutationDirection getDirection() {
        return directionRef.get();
    }

    public void setEvent(FundsMutationEvent event) {
        eventBuilder.setFundsMutationEvent(event);
    }

    public void setNaturalRate(BigDecimal naturalRate) {
        this.naturalRateRef = Optional.of(naturalRate);
    }

    @Nullable
    public BigDecimal getNaturalRate() {
        return naturalRateRef.orElseGet(null);
    }

    @Override
    public Money getAmount() {
        return amountWrapper.getAmount();
    }

    @Override
    public void setAmount(Money amount) {
        amountWrapper.setAmount(amount);
        adjustRelevantBalance(amount.getCurrencyUnit(), MutationDirection.BENEFIT);
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        amountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        setAmountUnit(CurrencyUnit.of(code));
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        amountWrapper.setAmountUnit(unit);
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

    public void setRelevantBalance(Treasury.BalanceAccount relevantBalance) {
        eventBuilder.setRelevantBalance(relevantBalance);
        if (directionRef.isPresent()) {
            adjustUnitsUsingBalance(directionRef.get(), relevantBalance);
        }
    }

    @Nullable
    public Treasury.BalanceAccount getRelevantBalance() {
        return eventBuilder.getRelevantBalance();
    }

    public void setPayeeAccountUnit(String code) {
        setPayeeAccountUnit(CurrencyUnit.of(code));
    }

    public void setPayeeAccountUnit(CurrencyUnit unit) {
        payeeAccountMoneyWrapper.setAmountUnit(unit);
        adjustRelevantBalance(unit, MutationDirection.LOSS);
    }

    @Nullable
    public CurrencyUnit getPayeeAccountUnit() {
        return payeeAccountMoneyWrapper.getAmountUnit();
    }

    public void setPaidMoney(Money money) {
        payeeAccountMoneyWrapper.setAmount(money);
        adjustRelevantBalance(money.getCurrencyUnit(), MutationDirection.LOSS);
    }

    public Money getPaidMoney() {
        return payeeAccountMoneyWrapper.getAmount();
    }

    public void setPayeeAmount(int coins, int cents) {
        payeeAccountMoneyWrapper.setAmount(coins, cents);
    }

    public void setPayeeAmount(BigDecimal amount) {
        payeeAccountMoneyWrapper.setAmountDecimal(amount);
    }

    public BigDecimal getPayeeAmount() {
        return payeeAccountMoneyWrapper.getAmountDecimal();
    }

    public void setCustomRate(BigDecimal customRate) {
        this.customRateRef = Optional.ofNullable(customRate);
    }

    @Nullable
    public BigDecimal getCustomRate() {
        return customRateRef.orElse(null);
    }

    public void setQuantity(int quantity) {
        eventBuilder.setQuantity(quantity);
    }

    public int getQuantity() {
        return eventBuilder.getQuantity();
    }

    public void setSubject(String subjectName) {
        eventBuilder.setSubject(accounter.fundsMutationSubjectRepo().findByName(subjectName).orElseThrow(NullPointerException::new));
    }

    public void setSubject(FundsMutationSubject subject) {
        eventBuilder.setSubject(subject);
    }

    @Nullable
    public FundsMutationSubject getSubject() {
        return eventBuilder.getSubject();
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        eventBuilder.setTimestamp(timestamp);
    }

    @Nullable
    public OffsetDateTime getTimestamp() {
        return eventBuilder.getTimestamp();
    }

    public void setMutateFunds(boolean mutateFunds) {
        this.mutateFunds = mutateFunds;
    }

    public boolean getMutateFunds() {
        return mutateFunds;
    }

    public void setAgent(FundsMutationAgent agent) {
        eventBuilder.setAgent(agent);
    }

    public void setAgentString(String agentStr) {
        final FundsMutationAgentRepository repo = accounter.fundsMutationAgentRepo();
        final Optional<FundsMutationAgent> byName = repo.findByName(agentStr);
        eventBuilder.setAgent(byName.orElseGet(() -> {
            final FundsMutationAgent agent = FundsMutationAgent.builder().setName(agentStr).build();
            repo.addAgent(agent);
            return agent;
        }));
    }

    @Nullable
    public FundsMutationAgent getAgent() {
        return eventBuilder.getAgent();
    }

    /**
     * Orientation is: [amount] = [paid amount] * rate.
     */
    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(directionRef, FIELD_DIRECTION)
                .addFieldErrorIfNull(eventBuilder.getRelevantBalance(), FIELD_RELEVANT_BALANCE)
                .addFieldErrorIfNull(eventBuilder.getAgent(), FIELD_AGENT)
                .addFieldErrorIfNull(eventBuilder.getSubject(), FIELD_SUBJECT)
                .addFieldErrorIfNull(eventBuilder.getTimestamp(), FIELD_TIMESTAMP);

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
            resultBuilder.addFieldError(FIELD_QUANTITY, "Fill in positive");
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        try {
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
                    amount = (sameUnits)
                            ? paidAmount
                            : paidAmount.convertedTo(amountUnit, customRateRef.orElseGet(() -> calculateNaturalRate(paidUnit, amountUnit)));
                } else {
                    // actual amount was set explicitly
                    amount = amountWrapper.getAmount().toBigMoney();
                }

                if (!sameUnits) {
                    // currency conversion to be
                    final BigDecimal naturalRate = calculateNaturalRate(paidUnit, amountUnit);
                    final Money amountSmallMoney = amount.toMoney(RoundingMode.HALF_DOWN);
                    if (naturalRate == null) {
                        // we don't have today's rates yet, do accounting later
                        direction.remember(accounter, eventBuilder.setAmount(amountSmallMoney).build(), paidUnit, customRateRef);
                        return Result.success(null);
                    }

                    final BigDecimal actualRate = customRateRef.orElse(naturalRate);
                    final BigMoney soldAmount = payeeAccountMoneyWrapper.isAmountSet()
                            ? payeeAccountMoneyWrapper.getAmount().toBigMoney()
                            : amount.convertedTo(paidUnit, CurrencyRatesProvider.reverseRate(actualRate));

                    final Money soldAmountSmallMoney = soldAmount.toMoney(RoundingMode.HALF_DOWN);
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

                    direction.register(accounter, treasury, eventBuilder, appropriateMutationAmount, mutateFunds);
                    accounter.registerCurrencyExchange(
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
                    return Result.success(null);
                }
            } else {
                amount = amountWrapper.getAmount().toBigMoney();
            }

            direction.register(accounter, treasury, eventBuilder, amount.toMoney(), mutateFunds);
        } catch (RuntimeException ex) {
            logger.error("Error while performing funds mutation business logic", ex);
            return resultBuilder
                    .setGeneralError("Error while performing funds mutation business logic: " + ex.getMessage())
                    .build();
        }

        return Result.success(null);
    }

    private BigDecimal calculateNaturalRate(CurrencyUnit paidUnit, CurrencyUnit amountUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(eventBuilder.getTimestamp()), paidUnit, amountUnit).orElse(null));
        }
        return calculatedNaturalRate;
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

    private void adjustUnitsUsingBalance(MutationDirection dir, Treasury.BalanceAccount relevantBalance) {
        final CurrencyUnit relevantBalanceUnit = relevantBalance.getUnit();
        switch (dir) {
            case LOSS:
                setPayeeAccountUnit(relevantBalanceUnit);
                break;
            case BENEFIT:
                setAmountUnit(relevantBalanceUnit);
        }
    }

    private void adjustRelevantBalance(CurrencyUnit unit, MutationDirection dir) {
        final Treasury.BalanceAccount relevantBalance = getRelevantBalance();
        if (directionRef.isPresent() && relevantBalance != null && directionRef.get() == dir && !unit.equals(relevantBalance.getUnit())) {
            eventBuilder.setRelevantBalance(null);
        }
    }

}
