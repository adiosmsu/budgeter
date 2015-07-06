package ru.adios.budgeter;

import ru.adios.budgeter.api.UtcDay;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Date: 03.07.15
 * Time: 22:46
 *
 * @author Mikhail Kulikov
 */
public final class TestUtils {

    public static final UtcDay TODAY = new UtcDay();
    public static final UtcDay YESTERDAY = new UtcDay(TODAY.inner.minus(1, ChronoUnit.DAYS));
    public static final UtcDay DAY_BF_YESTER = new UtcDay(YESTERDAY.inner.minus(1, ChronoUnit.DAYS));

    public static final UtcDay JULY_3RD_2015 = new UtcDay(OffsetDateTime.of(2015, 7, 3, 0, 0, 0, 0, ZoneOffset.UTC));

    private TestUtils() {}

}
