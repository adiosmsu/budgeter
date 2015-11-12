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

import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;
import ru.adios.budgeter.DateTimeUtils;

import javax.annotation.Nonnull;

/**
 * Date: 6/14/15
 * Time: 3:24 AM
 *
 * @author Mikhail Kulikov
 */
public final class UtcDay implements Comparable<UtcDay> {

    public final OffsetDateTime inner;

    public UtcDay() {
        inner = DateTimeUtils.cutTime(DateTimeUtils.nowInUtc());
    }

    public UtcDay(OffsetDateTime inner) {
        this.inner = DateTimeUtils.toUtcDay(inner);
    }

    public UtcDay(String str, DateTimeFormatter formatter) {
        inner = DateTimeUtils.cutTime(OffsetDateTime.parse(str, formatter.withZone(ZoneOffset.UTC)));
    }

    public UtcDay(long millisFromEpoch) {
        inner = DateTimeUtils.fromEpochMillis(millisFromEpoch, ZoneOffset.UTC);
    }

    private UtcDay(OffsetDateTime calculated, boolean constructorMarker) {
        inner = calculated;
    }

    public UtcDay add(int days) {
        if (days == 0) {
            return this;
        } else if (days > 0) {
            return new UtcDay(inner.plus(days, ChronoUnit.DAYS), true);
        } else {
            return new UtcDay(inner.minus(-days, ChronoUnit.DAYS), true);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || !(o == null || getClass() != o.getClass())
                && inner.equals(((UtcDay) o).inner);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    @Override
    public int compareTo(@Nonnull UtcDay other) {
        return inner.compareTo(other.inner);
    }

}
