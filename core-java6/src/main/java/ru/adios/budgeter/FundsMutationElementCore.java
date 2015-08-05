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

    public void setPostponedEvent(PostponedFundsMutationEventRepository.PostponedMutationEvent event, BigDecimal naturalRate) {
        setEvent(event.mutationEvent);
        setAmount(event.mutationEvent.amount);
        setDirection(MutationDirection.forEvent(event.mutationEvent));
        setPayeeAccountUnit(event.conversionUnit);
        setCustomRate(event.customRate.orElse(null));
        setNaturalRate(naturalRate);
        setTimestamp(event.mutationEvent.timestamp);
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

    @Override
    public void setAmount(int coins, int cents) {
        amountWrapper.setAmount(coins, cents);
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

    public void setPayeeAmount(int coins, int cents) {
        payeeAccountMoneyWrapper.setAmount(coins, cents);
    }

    public void setPayeeAmount(BigDecimal amount) {
        payeeAccountMoneyWrapper.setAmountDecimal(amount);
    }

    public void setCustomRate(BigDecimal customRate) {
        this.customRateRef = Optional.ofNullable(customRate);
    }

    public void setQuantity(int quantity) {
        eventBuilder.setQuantity(quantity);
    }

    public void setSubject(String subjectName) {
        eventBuilder.setSubject(accounter.fundsMutationSubjectRepo().findByName(subjectName).orElseThrow(new Supplier<NullPointerException>() {
            @Override
            public NullPointerException get() {
                return new NullPointerException();
            }
        }));
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

    public void setAgentString(final String agentStr) {
        final FundsMutationAgentRepository repo = accounter.fundsMutationAgentRepo();
        final Optional<FundsMutationAgent> byName = repo.findByName(agentStr);
        eventBuilder.setAgent(byName.orElseGet(new Supplier<FundsMutationAgent>() {
            @Override
            public FundsMutationAgent get() {
                final FundsMutationAgent agent = FundsMutationAgent.builder().setName(agentStr).build();
                repo.addAgent(agent);
                return agent;
            }
        }));
    }

    /**
     * Orientation is: [amount] = [payed amount] * rate.
     */
    @Override
    public void submit() {
        checkState(directionRef.isPresent(), "No direction set");
        final MutationDirection direction = directionRef.get();
        final CurrencyUnit amountUnit = amountWrapper.getAmountUnit();

        final BigMoney amount;
        if (payeeAccountMoneyWrapper.isUnitSet()) {
            // and so actual account of payment was set (even if only as a currency)
            final CurrencyUnit payedUnit = payeeAccountMoneyWrapper.getAmountUnit(); // therefore we know it for sure
            final boolean sameUnits = payedUnit.equals(amountUnit);

            if (!customRateRef.isPresent() && payeeAccountMoneyWrapper.isAmountSet() && amountWrapper.isAmountSet()) {
                // we know about payed money and actual amount both therefore we must calculate custom rate which with 99% chance will differ from natural rate
                customRateRef = Optional.of(CurrencyRatesProvider.Static.calculateRate(amountWrapper.getAmount().getAmount(), payeeAccountMoneyWrapper.getAmount().getAmount()));
            }

            if (!amountWrapper.isAmountSet()) {
                // actual amount wasn't set explicitly so we must calculate it from payed amount and rate (custom or natural) which must have been provided
                final BigMoney payedAmount = payeeAccountMoneyWrapper.getAmount().toBigMoney();
                amount = (sameUnits)
                        ? payedAmount
                        : payedAmount.convertedTo(amountUnit, customRateRef.orElseGet(new Supplier<BigDecimal>() {
                    @Override
                    public BigDecimal get() {
                        return calculateNaturalRate(payedUnit, amountUnit);
                    }
                }));
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
                    return;
                }

                final BigDecimal actualRate = customRateRef.orElse(naturalRate);
                final BigMoney soldAmount = payeeAccountMoneyWrapper.isAmountSet()
                        ? payeeAccountMoneyWrapper.getAmount().toBigMoney()
                        : amount.convertedTo(payedUnit, CurrencyRatesProvider.Static.reverseRate(actualRate));

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
                        naturalAmount = amount.convertedTo(payedUnit, CurrencyRatesProvider.Static.reverseRate(naturalRate));
                        convertedAmount = soldAmount;
                    }

                    FundsMutator.Static.registerExchangeDifference(
                            this,
                            naturalAmount.toMoney(RoundingMode.HALF_DOWN),
                            convertedAmount.toMoney(RoundingMode.HALF_DOWN),
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
                                .setTimestamp(eventBuilder.getTimestamp())
                                .build()
                );
                return;
            }
        } else {
            amount = amountWrapper.getAmount().toBigMoney();
        }

        direction.register(accounter, treasury, eventBuilder, amount.toMoney(), mutateFunds);
    }

    private BigDecimal calculateNaturalRate(final CurrencyUnit payedUnit, final CurrencyUnit amountUnit) {
        if (calculatedNaturalRate == null) {
            calculatedNaturalRate = naturalRateRef.orElseGet(new Supplier<BigDecimal>() {
                @Override
                public BigDecimal get() {
                    return ratesService.getConversionMultiplier(new UtcDay(eventBuilder.getTimestamp()), payedUnit, amountUnit).orElse(null);
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
    public Treasury getTreasury() {
        return treasury;
    }

    @Override
    public CurrenciesExchangeService getRatesService() {
        return ratesService;
    }

}
