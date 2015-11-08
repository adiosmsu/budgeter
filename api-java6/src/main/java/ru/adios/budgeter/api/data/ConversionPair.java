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
