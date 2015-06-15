package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;
import java.util.Optional;

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
        return amountRef.orElseGet(this::initAmount);
    }

    private Money initAmount() {
        checkState(amountDecimalRef.isPresent() && amountUnitRef.isPresent(),
                "Unable to initialize money instance (%s) without decimal amount and currency unit", name);
        final Money amount = Money.of(amountUnitRef.get(), amountDecimalRef.get());
        amountRef = Optional.of(amount);
        return amount;
    }

}
