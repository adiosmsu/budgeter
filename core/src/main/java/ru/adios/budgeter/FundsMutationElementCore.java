package ru.adios.budgeter;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Date: 6/13/15
 * Time: 3:00 AM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public final class FundsMutationElementCore implements MoneySettable, FundsMutator, Submitter {

    private final Accounter accounter;
    private final CurrenciesExchangeService ratesService;
    private final Treasury treasury;

    private final MoneyWrapperBean amountWrapper = new MoneyWrapperBean("funds mutation amount");
    private final MoneyWrapperBean payeeAccountMoneyWrapper = new MoneyWrapperBean("funds mutation payed amount");

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

    public CurrencyUnit getPayeeAccountUnit() {
        return payeeAccountMoneyWrapper.getAmountUnit();
    }

    public void setPayedMoney(Money money) {
        payeeAccountMoneyWrapper.setAmount(money);
        adjustRelevantBalance(money.getCurrencyUnit(), MutationDirection.LOSS);
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

    public FundsMutationSubject getSubject() {
        return eventBuilder.getSubject();
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        eventBuilder.setTimestamp(timestamp);
    }

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

    public FundsMutationAgent getAgent() {
        return eventBuilder.getAgent();
    }

    /**
     * Orientation is: [amount] = [payed amount] * rate.
     */
    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(directionRef, "direction")
                .addFieldErrorIfNull(eventBuilder.getRelevantBalance(), "relevantBalance")
                .addFieldErrorIfNull(eventBuilder.getAgent(), "agent")
                .addFieldErrorIfNull(eventBuilder.getSubject(), "subject");

        if (!amountWrapper.isUnitSet()) {
            resultBuilder.addFieldError("amountUnit")
                    .addFieldError("amount");
        }

        if (payeeAccountMoneyWrapper.isUnitSet() && !amountWrapper.isAmountSet() && !payeeAccountMoneyWrapper.isAmountSet()) {
            resultBuilder.addFieldError("amount")
                    .addFieldError("amountDecimal")
                    .addFieldError("payeeAmount")
                    .addFieldError("payedMoney");
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        final MutationDirection direction = directionRef.get();
        final CurrencyUnit amountUnit = amountWrapper.getAmountUnit();

        final BigMoney amount;
        if (payeeAccountMoneyWrapper.isUnitSet()) {
            // and so actual account of payment was set (even if only as a currency)
            final CurrencyUnit payedUnit = payeeAccountMoneyWrapper.getAmountUnit(); // therefore we know it for sure
            final boolean sameUnits = payedUnit.equals(amountUnit);

            if (!customRateRef.isPresent() && payeeAccountMoneyWrapper.isAmountSet() && amountWrapper.isAmountSet()) {
                // we know about payed money and actual amount both therefore we must calculate custom rate which with 99% chance will differ from natural rate
                customRateRef = Optional.of(CurrencyRatesProvider.calculateRate(amountWrapper.getAmount().getAmount(), payeeAccountMoneyWrapper.getAmount().getAmount()));
            }

            if (!amountWrapper.isAmountSet()) {
                // actual amount wasn't set explicitly so we must calculate it from payed amount and rate (custom or natural) which must have been provided
                final BigMoney payedAmount = payeeAccountMoneyWrapper.getAmount().toBigMoney();
                amount = (sameUnits)
                        ? payedAmount
                        : payedAmount.convertedTo(amountUnit, customRateRef.orElseGet(() -> calculateNaturalRate(payedUnit, amountUnit)));
            } else {
                // actual amount was set explicitly
                amount = amountWrapper.getAmount().toBigMoney();
            }

            if (!sameUnits) {
                // currency conversion to be
                final BigDecimal naturalRate = calculateNaturalRate(payedUnit, amountUnit);
                final Money amountSmallMoney = amount.toMoney(RoundingMode.HALF_DOWN);
                if (naturalRate == null) {
                    // we don't have today's rates yet, do accounting later
                    direction.remember(accounter, eventBuilder.setAmount(amountSmallMoney).build(), payedUnit, customRateRef);
                    return Result.SUCCESS;
                }

                final BigDecimal actualRate = customRateRef.orElse(naturalRate);
                final BigMoney soldAmount = payeeAccountMoneyWrapper.isAmountSet()
                        ? payeeAccountMoneyWrapper.getAmount().toBigMoney()
                        : amount.convertedTo(payedUnit, CurrencyRatesProvider.reverseRate(actualRate));

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
                        naturalAmount = amount.convertedTo(payedUnit, CurrencyRatesProvider.reverseRate(naturalRate));
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
                return Result.SUCCESS;
            }
        } else {
            amount = amountWrapper.getAmount().toBigMoney();
        }

        direction.register(accounter, treasury, eventBuilder, amount.toMoney(), mutateFunds);
        return Result.SUCCESS;
    }

    private BigDecimal calculateNaturalRate(CurrencyUnit payedUnit, CurrencyUnit amountUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(eventBuilder.getTimestamp()), payedUnit, amountUnit).orElse(null));
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
