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

package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 10:02 PM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyRatesRepository extends CurrencyRatesProvider {

    /**
     * Add a today's rate to repository.
     * @param from   from what currency exchange is happening
     * @param to     to what currency exchange is happening
     * @param rate   rate as decimal number
     * @return true if success, false otherwise
     */
    default boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return addRate(new UtcDay(), from, to, rate);
    }

    /**
     * Add a rate to repository.
     * @param dayUtc day of the rate
     * @param from   from what currency exchange is happening
     * @param to     to what currency exchange is happening
     * @param rate   rate as decimal number
     * @return true if success, false otherwise
     */
    boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate);

}
