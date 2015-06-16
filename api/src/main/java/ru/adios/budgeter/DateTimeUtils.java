package ru.adios.budgeter;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

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

    private DateTimeUtils() {}

}
