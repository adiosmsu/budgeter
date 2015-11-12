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

import org.joda.money.CurrencyUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 11/3/15
 * Time: 11:02 AM
 *
 * @author Mikhail Kulikov
 */
public final class ConversionPair {

    public final CurrencyUnit from;
    public final CurrencyUnit to;

    public ConversionPair(CurrencyUnit from, CurrencyUnit to) {
        checkNotNull(from, "from");
        checkNotNull(to, "to");

        this.from = from;
        this.to = to;
    }

    public boolean containsIgnoreOrder(CurrencyUnit first, CurrencyUnit second) {
        return (from.equals(first) && to.equals(second))
                || (from.equals(second) && to.equals(first));
    }

    public StringBuilder appendTo(StringBuilder sb) {
        return sb.append(from).append('/').append(to);
    }

    @Override
    public String toString() {
        return from.toString() + '/' + to.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversionPair that = (ConversionPair) o;

        return from.equals(that.from)
                && to.equals(that.to);
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }

}
