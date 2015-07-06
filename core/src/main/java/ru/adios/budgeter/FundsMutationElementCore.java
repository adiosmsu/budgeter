package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 3:00 AM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationElementCore implements MoneySettable, FundsMutator, Submitter {

    private final Accounter accounter;
    private final CurrenciesExchangeService ratesService;
    private final Treasury treasury;

    private final MoneyWrapperBean amountWrapper = new MoneyWrapperBean("funds mutation amount");
    private final MoneyWrapperBean payeeAccountMoneyWrapper = new MoneyWrapperBean("funds mutation payed amount");

    private Optional<MutationDirection> directionRef = Optional.empty();
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

    public void setDirection(MutationDirection direction) {
        this.directionRef = Optional.of(direction);
    }

    public void setEvent(FundsMutationEvent event) {
        eventBuilder.setFundsMutationEvent(event);
    }

    public void setNaturalRate(BigDecimal naturalRate) {
        this.naturalRateRef = Optional.of(naturalRate);
    }

    @Override
    public Money getAmount() {
        return amountWrapper.getAmount();
    }

    @Override
    public void setAmount(Money amount) {
        amountWrapper.setAmount(amount);
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        amountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        amountWrapper.setAmountUnit(code);
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        amountWrapper.setAmountUnit(unit);
    }

    public void setPayeeAccountUnit(String code) {
        payeeAccountMoneyWrapper.setAmountUnit(CurrencyUnit.of(code));
    }

    public void setPayeeAccountUnit(CurrencyUnit unit) {
        payeeAccountMoneyWrapper.setAmountUnit(unit);
    }

    public void setPayedMoney(Money money) {
        payeeAccountMoneyWrapper.setAmount(money);
    }

    public void setPayeeAmount(BigDecimal amount) {
        payeeAccountMoneyWrapper.setAmountDecimal(amount);
    }

    public void setCustomRate(Optional<BigDecimal> customRateRef) {
        this.customRateRef = customRateRef;
    }

    public void setQuantity(int quantity) {
        eventBuilder.setQuantity(quantity);
    }

    public void setSubject(String subjectName) {
        eventBuilder.setSubject(accounter.fundsMutationSubjectRepo().findByName(subjectName).orElseThrow(NullPointerException::new));
    }

    public void setSubject(FundsMutationSubject subject) {
        eventBuilder.setSubject(subject);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        eventBuilder.setTimestamp(timestamp);
    }

    public void setMutateFunds(boolean mutateFunds) {
        this.mutateFunds = mutateFunds;
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

    public void submit() {
        checkState(directionRef.isPresent(), "No direction set");
        final MutationDirection direction = directionRef.get();
        final CurrencyUnit amountUnit = amountWrapper.getAmountUnit();

        final Money amount;
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
                final Money payedAmount = payeeAccountMoneyWrapper.getAmount();
                amount = (sameUnits)
                        ? payedAmount
                        : payedAmount.convertedTo(amountUnit, customRateRef.orElseGet(() -> calculateNaturalRate(amountUnit, payedUnit)), RoundingMode.HALF_DOWN);
            } else {
                // actual amount was set explicitly
                amount = amountWrapper.getAmount();
            }

            if (!sameUnits) {
                // currency conversion to be
                final BigDecimal naturalRate = calculateNaturalRate(amountUnit, payedUnit);
                if (naturalRate == null) {
                    // we don't have today's rates yet, do accounting later
                    direction.remember(accounter, eventBuilder.setAmount(amount).build(), payedUnit, customRateRef);
                    return;
                }

                final BigDecimal actualRate = customRateRef.orElse(naturalRate);
                final Money soldAmount = payeeAccountMoneyWrapper.isAmountSet()
                        ? payeeAccountMoneyWrapper.getAmount()
                        : amount.convertedTo(payedUnit, CurrencyRatesProvider.reverseRate(actualRate), RoundingMode.HALF_DOWN);
                if (customRateRef.isPresent()) {
                    // custom exchange rate present, will need to calculate difference and account it
                    final Money naturalAmount = soldAmount.convertedTo(amountUnit, naturalRate, RoundingMode.HALF_DOWN);
                    final Money convertedAmount = soldAmount.convertedTo(amountUnit, customRateRef.get(), RoundingMode.HALF_DOWN);

                    FundsMutator.registerExchangeDifference(
                            this,
                            naturalAmount,
                            convertedAmount,
                            direction,
                            eventBuilder.getAgent(),
                            eventBuilder.getTimestamp(),
                            eventBuilder.getQuantity()
                    );
                }

                direction.register(accounter, treasury, eventBuilder, amount, mutateFunds);
                accounter.registerCurrencyExchange(
                        CurrencyExchangeEvent.builder()
                                .setRate(actualRate)
                                .setBought(amount)
                                .setSold(soldAmount)
                                .setTimestamp(eventBuilder.getTimestamp())
                                .build()
                );
                return;
            }
        } else {
            amount = amountWrapper.getAmount();
        }

        direction.register(accounter, treasury, eventBuilder, amount, mutateFunds);
    }

    private BigDecimal calculateNaturalRate(CurrencyUnit amountUnit, CurrencyUnit payedUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(eventBuilder.getTimestamp()), amountUnit, payedUnit).orElse(null));
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

}
