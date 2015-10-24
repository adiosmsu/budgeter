package ru.adios.budgeter;

import org.threeten.bp.OffsetDateTime;

import javax.annotation.Nullable;

/**
 * Date: 10/24/15
 * Time: 4:44 PM
 *
 * @author Mikhail Kulikov
 */
public interface TimestampSettable {

    @Nullable
    OffsetDateTime getTimestamp();

    void setTimestamp(@Nullable OffsetDateTime ts);

}
