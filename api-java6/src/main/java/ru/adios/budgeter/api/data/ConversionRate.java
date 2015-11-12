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

package ru.adios.budgeter.api.data;

import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 11/3/15
 * Time: 11:02 AM
 *
 * @author Mikhail Kulikov
 */
public final class ConversionRate {

    public final UtcDay day;
    public final ConversionPair pair;
    public final BigDecimal rate;

    public ConversionRate(UtcDay day, ConversionPair pair, BigDecimal rate) {
        checkNotNull(day, "day");
        checkNotNull(pair, "pair");
        checkNotNull(rate, "rate");
        this.day = day;
        this.pair = pair;
        this.rate = rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversionRate that = (ConversionRate) o;

        return day.equals(that.day)
                && pair.equals(that.pair)
                && rate.equals(that.rate);
    }

    @Override
    public int hashCode() {
        int result = day.hashCode();
        result = 31 * result + pair.hashCode();
        result = 31 * result + rate.hashCode();
        return result;
    }

}
