package ru.adios.budgeter;

import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 6/13/15
 * Time: 5:00 PM
 *
 * @author Mikhail Kulikov
 */
public interface MoneySettable {

    final class Static {

        public static void setArbitraryMoney(int coins, int cents, Consumer<BigDecimal> setterRef) {
            checkArgument(coins >= 0 && cents >= 0 && (coins > 0 || cents > 0), "At least one must be strictly positive, other not negative");
            setterRef.accept(BigDecimal.valueOf(coins * 100 + cents, 2));
        }

    }

    final class Default {

        private final MoneySettable moneySettable;

        public Default(MoneySettable moneySettable) {
            this.moneySettable = moneySettable;
        }

        public void setAmount(int coins, int cents) {
            Static.setArbitraryMoney(coins, cents, new Consumer<BigDecimal>() {
                @Override
                public void accept(BigDecimal bigDecimal) {
                    moneySettable.setAmountDecimal(bigDecimal);
                }
            });
        }

    }

    void setAmount(int coins, int cents); // default in java8

    void setAmountDecimal(BigDecimal amountDecimal);

    void setAmountUnit(String code);

    void setAmountUnit(CurrencyUnit unit);

    void setAmount(Money amount);

    Money getAmount();

}
