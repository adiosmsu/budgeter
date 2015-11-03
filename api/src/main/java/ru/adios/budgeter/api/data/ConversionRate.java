package ru.adios.budgeter.api.data;

import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 11/3/15
 * Time: 11:03 AM
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
