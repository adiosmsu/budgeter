package ru.adios.budgeter.api;

import ru.adios.budgeter.DateTimeUtils;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
        inner = OffsetDateTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(millisFromEpoch), ZoneOffset.UTC), ZoneOffset.UTC);
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
