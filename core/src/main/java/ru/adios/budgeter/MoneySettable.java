/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

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

    BigDecimal getAmountDecimal();

    void setAmountUnit(String code);

    void setAmountUnit(CurrencyUnit unit);

    CurrencyUnit getAmountUnit();

    void setAmount(Money amount);

    Money getAmount();

}
