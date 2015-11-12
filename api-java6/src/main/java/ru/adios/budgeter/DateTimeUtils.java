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

import org.threeten.bp.*;
import org.threeten.bp.temporal.ChronoField;

import java.util.TimeZone;

/**
 * Date: 6/14/15
 * Time: 2:49 AM
 *
 * @author Mikhail Kulikov
 */
public final class DateTimeUtils {

    public static OffsetDateTime cutTime(OffsetDateTime dateTime) {
        return dateTime.with(ChronoField.NANO_OF_DAY, 0L);
    }

    public static OffsetDateTime nowInUtc() {
        return OffsetDateTime.now(Clock.systemUTC());
    }

    public static OffsetDateTime toUtcDay(OffsetDateTime dateTime) {
        return DateTimeUtils.cutTime(dateTime.withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static OffsetDateTime fromEpochMillis(long millisFromEpoch, ZoneId offset) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millisFromEpoch), offset);
    }

    public static OffsetDateTime convertToCurrentZone(OffsetDateTime odt) {
        return odt.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(TimeZone.getDefault().getRawOffset() / 1000));
    }

    private DateTimeUtils() {}

}
