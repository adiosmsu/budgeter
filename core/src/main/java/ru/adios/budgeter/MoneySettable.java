package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 6/13/15
 * Time: 5:00 PM
 *
 * @author Mikhail Kulikov
 */
public interface MoneySettable {

    static void setArbitraryMoney(int coins, int cents, Consumer<BigDecimal> setterRef) {
        checkArgument(coins >= 0 && cents >= 0 && (coins > 0 || cents > 0), "At least one must be strictly positive, other not negative");
        setterRef.accept(BigDecimal.valueOf(coins * 100 + cents, 2));
    }

    default void setAmount(int coins, int cents) {
        setArbitraryMoney(coins, cents, this::setAmountDecimal);
    }

    void setAmountDecimal(BigDecimal amountDecimal);

    void setAmountUnit(String code);

    void setAmountUnit(CurrencyUnit unit);

    void setAmount(Money amount);

    Money getAmount();

}
