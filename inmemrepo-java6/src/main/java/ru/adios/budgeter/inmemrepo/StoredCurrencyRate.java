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

package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.ConversionPair;
import ru.adios.budgeter.api.data.ConversionRate;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 7:19 PM
 *
 * @author Mikhail Kulikov
 */
final class StoredCurrencyRate extends Stored<UtcDay> {

    final CurrencyUnit first;
    final CurrencyUnit second;
    final BigDecimal rate;

    StoredCurrencyRate(int id, UtcDay obj, CurrencyUnit first, CurrencyUnit second, BigDecimal rate) {
        super(id, obj);
        this.first = first;
        this.second = second;
        this.rate = rate;
    }

    ConversionRate createConversionRate() {
        return new ConversionRate(obj, new ConversionPair(first, second), rate);
    }

}
