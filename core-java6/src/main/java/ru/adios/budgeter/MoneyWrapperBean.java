package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Supplier;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 8:25 PM
 *
 * @author Mikhail Kulikov
 */
public final class MoneyWrapperBean implements MoneySettable {

    public final String name;

    Optional<BigDecimal> amountDecimalRef = Optional.empty();
    Optional<CurrencyUnit> amountUnitRef = Optional.empty();
    Optional<Money> amountRef = Optional.empty();

    private final MoneySettable.Default msDef = new Default(this);

    public MoneyWrapperBean(String name) {
        this.name = name;
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        amountDecimalRef = Optional.of(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        setAmountUnit(CurrencyUnit.of(code));
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        checkNotNull(unit);
        if (amountUnitRef.isPresent() && !unit.equals(amountUnitRef.get())) {
            amountDecimalRef = Optional.empty();
        }
        if (amountRef.isPresent() && !unit.equals(amountRef.get().getCurrencyUnit())) {
            amountRef = Optional.empty();
        }
        amountUnitRef = Optional.of(unit);
    }

    @Override
    public void setAmount(Money amount) {
        checkNotNull(amount);
        amountRef = Optional.of(amount);
        amountDecimalRef = Optional.of(amount.getAmount());
        amountUnitRef = Optional.of(amount.getCurrencyUnit());
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return amountDecimalRef.orElseGet(new Supplier<BigDecimal>() {
            @Override
            public BigDecimal get() {
                return amountRef.isPresent() ? amountRef.get().getAmount() : BigDecimal.ZERO;
            }
        });
    }

    @Override
    public Money getAmount() {
        return amountRef.orElseGet(new Supplier<Money>() {
            @Override
            public Money get() {
                return initAmount();
            }
        });
    }

    @Override
    public void setAmount(int coins, int cents) {
        msDef.setAmount(coins, cents);
    }

    public CurrencyUnit getAmountUnit() {
        return amountUnitRef.orElseGet(new Supplier<CurrencyUnit>() {
            @Override
            public CurrencyUnit get() {
                return amountRef.isPresent() ? amountRef.get().getCurrencyUnit() : null;
            }
        });
    }

    public boolean isAmountSet() {
        return amountRef.isPresent() || amountDecimalRef.isPresent();
    }

    public boolean isUnitSet() {
        return amountRef.isPresent() || amountUnitRef.isPresent();
    }

    public boolean isInitiable() {
        return amountRef.isPresent() || (amountDecimalRef.isPresent() && amountUnitRef.isPresent());
    }

    private Money initAmount() {
        checkState(amountDecimalRef.isPresent() && amountUnitRef.isPresent(),
                "Unable to initialize money instance (%s) without decimal amount and currency unit", name);
        final Money amount = Money.of(amountUnitRef.get(), amountDecimalRef.get());
        amountRef = Optional.of(amount);
        return amount;
    }

}
