package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 8:25 PM
 *
 * @author Mikhail Kulikov
 */
public final class MoneyPositiveWrapper implements MoneySettable {

    public final String name;

    Optional<BigDecimal> amountDecimalRef = Optional.empty();
    Optional<CurrencyUnit> amountUnitRef = Optional.empty();
    Optional<Money> amountRef = Optional.empty();

    public MoneyPositiveWrapper(String name) {
        this.name = name;
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        if (amountDecimal == null) {
            amountDecimalRef = Optional.empty();
            if (amountRef.isPresent()) {
                amountUnitRef = Optional.of(amountRef.get().getCurrencyUnit());
                amountRef = Optional.empty();
            }
        } else {
            amountDecimalRef = Optional.of(amountDecimal);
        }
    }

    @Override
    public void setAmountUnit(String code) {
        if (code == null) {
            emptyAmountUnitRefs();
        } else {
            setAmountUnit(CurrencyUnit.of(code));
        }
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        if (unit == null) {
            emptyAmountUnitRefs();
        } else {
            if (amountUnitRef.isPresent() && !unit.equals(amountUnitRef.get())) {
                amountDecimalRef = Optional.empty();
            }
            if (amountRef.isPresent() && !unit.equals(amountRef.get().getCurrencyUnit())) {
                amountRef = Optional.empty();
            }
            amountUnitRef = Optional.of(unit);
        }
    }

    @Override
    public void setAmount(Money amount) {
        if (amount == null) {
            amountRef = Optional.empty();
            amountDecimalRef = Optional.empty();
            amountUnitRef = Optional.empty();
        } else {
            amountRef = Optional.of(amount);
            amountDecimalRef = Optional.of(amount.getAmount());
            amountUnitRef = Optional.of(amount.getCurrencyUnit());
        }
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return amountDecimalRef.orElseGet(() -> amountRef.isPresent() ? amountRef.get().getAmount() : BigDecimal.ZERO);
    }

    @Override
    public Money getAmount() {
        return amountRef.orElseGet(this::initAmount);
    }

    @Nullable
    public CurrencyUnit getAmountUnit() {
        return amountUnitRef.orElseGet(() -> amountRef.isPresent() ? amountRef.get().getCurrencyUnit() : null);
    }

    public boolean isAmountSet() {
        return (amountRef.isPresent() && amountRef.get().getAmount().compareTo(BigDecimal.ZERO) > 0)
                || (amountDecimalRef.isPresent() && amountDecimalRef.get().compareTo(BigDecimal.ZERO) > 0);
    }

    public boolean isUnitSet() {
        return amountRef.isPresent() || amountUnitRef.isPresent();
    }

    public boolean isInitiable() {
        return (amountRef.isPresent() && amountRef.get().getAmount().compareTo(BigDecimal.ZERO) > 0) ||
                (amountDecimalRef.isPresent() && amountDecimalRef.get().compareTo(BigDecimal.ZERO) > 0 && amountUnitRef.isPresent());
    }

    private void emptyAmountUnitRefs() {
        amountUnitRef = Optional.empty();
        if (amountRef.isPresent()) {
            amountDecimalRef = Optional.of(amountRef.get().getAmount());
            amountRef = Optional.empty();
        }
    }

    private Money initAmount() {
        checkState(amountDecimalRef.isPresent() && amountUnitRef.isPresent(),
                "Unable to initialize money instance (%s) without decimal amount and currency unit", name);
        final Money amount = Money.of(amountUnitRef.get(), amountDecimalRef.get());
        amountRef = Optional.of(amount);
        return amount;
    }

}
