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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 9/25/15
 * Time: 4:26 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public final class NoRateException extends BudgeterApiException {

    public final CurrencyUnit first;
    public final CurrencyUnit second;

    public NoRateException(CurrencyUnit first, CurrencyUnit second) {
        super("Unknown exchange rate between " + first + " and " + second);
        this.first = first;
        this.second = second;
    }

}
