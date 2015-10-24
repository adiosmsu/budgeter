package ru.adios.budgeter;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

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
