package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Supplier;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/13/15
 * Time: 8:25 PM
 *
 * @author Mikhail Kulikov
 */
public final class MoneyWrapperBean implements MoneySettable {

    public final String name;

    public Optional<BigDecimal> amountDecimalRef = Optional.empty();
    public Optional<CurrencyUnit> amountUnitRef = Optional.empty();
    public Optional<Money> amountRef = Optional.empty();

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
        amountUnitRef = Optional.of(CurrencyUnit.of(code));
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        amountUnitRef = Optional.of(unit);
    }

    @Override
    public void setAmount(Money amount) {
        amountRef = Optional.of(amount);
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
        checkState(isUnitSet(), "Unit unknown in %s", name);
        return amountUnitRef.orElseGet(new Supplier<CurrencyUnit>() {
            @Override
            public CurrencyUnit get() {
                return amountRef.get().getCurrencyUnit();
            }
        });
    }

    public boolean isAmountSet() {
        return amountRef.isPresent() || amountDecimalRef.isPresent();
    }

    public boolean isUnitSet() {
        return amountRef.isPresent() || amountUnitRef.isPresent();
    }

    private Money initAmount() {
        checkState(amountDecimalRef.isPresent() && amountUnitRef.isPresent(),
                "Unable to initialize money instance (%s) without decimal amount and currency unit", name);
        final Money amount = Money.of(amountUnitRef.get(), amountDecimalRef.get());
        amountRef = Optional.of(amount);
        return amount;
    }

}
