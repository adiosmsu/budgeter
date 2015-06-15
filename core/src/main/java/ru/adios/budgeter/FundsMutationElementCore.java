package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 3:00 AM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationElementCore implements MoneySettable, FundsMutator {

    private final Accounter accounter;
    private final CurrenciesExchangeService ratesService;
    private final Treasury treasury;

    private final MoneyWrapperBean amountWrapper = new MoneyWrapperBean("funds mutation amount");

    private Optional<MutationDirection> directionRef = Optional.empty();
    private FundsMutationEvent.Builder eventBuilder = FundsMutationEvent.builder();
    private Optional<CurrencyUnit> convUnitRef = Optional.empty();
    private Optional<BigDecimal> customRateRef = Optional.empty();
    private Optional<BigDecimal> naturalRateRef = Optional.empty();

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

    public void setConversionUnit(String code) {
        convUnitRef = Optional.of(CurrencyUnit.of(code));
    }

    public void setConversionUnit(CurrencyUnit unit) {
        convUnitRef = Optional.of(unit);
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

    public void register() {
        checkState(directionRef.isPresent(), "No direction set");

        final MutationDirection direction = directionRef.get();
        final Money amount = getAmount();
        final CurrencyUnit amountUnit = amount.getCurrencyUnit();
        if (convUnitRef.isPresent()) {
            final CurrencyUnit unitConv = convUnitRef.get();
            if (!unitConv.equals(amountUnit)) {
                // currency conversion to be
                final BigDecimal naturalRate = naturalRateRef.orElseGet(() -> ratesService.getConversionMultiplier(new UtcDay(), amountUnit, unitConv).orElse(null));
                if (naturalRate == null) {
                    // we don't have today's rates yet, do accounting later
                    direction.remember(accounter, eventBuilder.setAmount(amount).build(), unitConv, customRateRef);
                    return;
                }

                final Money actualAmount, otherPartyAmount;
                final BigDecimal actualRate;
                if (customRateRef.isPresent()) {
                    // custom exchange rate present, will need to calculate difference and account it
                    actualRate = customRateRef.get();
                    actualAmount = direction.getActualAmount(amount, unitConv, actualRate);
                    otherPartyAmount = direction.getOtherPartyAmount(amount, unitConv, actualRate);

                    final Money naturalAmount = amount.convertedTo(unitConv, naturalRate, RoundingMode.HALF_DOWN);
                    final Money convertedAmount = direction.getConvertedAmount(actualAmount, otherPartyAmount);

                    FundsMutator.registerExchangeDifference(this, naturalAmount, convertedAmount, direction, eventBuilder.getQuantity());
                } else {
                    actualRate = naturalRate;
                    actualAmount = direction.getActualAmount(amount, unitConv, actualRate);
                    otherPartyAmount = direction.getOtherPartyAmount(amount, unitConv, actualRate);
                }

                direction.register(accounter, treasury, eventBuilder.setAmount(actualAmount).build());
                accounter.registerCurrencyExchange(
                        CurrencyExchangeEvent.builder()
                                .setRate(actualRate)
                                .setBought(actualAmount)
                                .setSold(otherPartyAmount)
                                .build()
                );
                return;
            }
        }

        direction.register(accounter, treasury, eventBuilder.setAmount(amount).build());
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
