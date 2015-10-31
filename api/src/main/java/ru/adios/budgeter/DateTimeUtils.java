package ru.adios.budgeter;

import java.time.*;
import java.time.temporal.ChronoField;
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
