package ru.adios.budgeter.api;

import org.joda.money.Money;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 6/13/15
 * Time: 3:11 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class CurrencyExchangeEvent {

    public static Builder builder() {
        return new Builder();
    }

    private CurrencyExchangeEvent(Builder builder) {
        this.bought = builder.bought;
        this.sold = builder.sold;
        this.rate = builder.rate;
        this.timestamp = builder.timestamp;
        checkArgument(bought != null && sold != null && rate != null, "Bad data, possibly uninitialized");
    }

    public final Money sold;
    public final Money bought;
    public final BigDecimal rate;
    public final OffsetDateTime timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrencyExchangeEvent that = (CurrencyExchangeEvent) o;

        return sold.equals(that.sold)
                && bought.equals(that.bought)
                && rate.equals(that.rate)
                && timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = sold.hashCode();
        result = 31 * result + bought.hashCode();
        result = 31 * result + rate.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    @NotThreadSafe
    public static final class Builder {

        private Money sold;
        private Money bought;
        private BigDecimal rate;
        private OffsetDateTime timestamp = OffsetDateTime.now();

        private Builder() {
        }

        public Builder setBought(Money bought) {
            this.bought = bought;
            return this;
        }

        public Builder setRate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public Builder setSold(Money sold) {
            this.sold = sold;
            return this;
        }

        public Builder setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = checkNotNull(timestamp);
            return this;
        }

        public CurrencyExchangeEvent build() {
            return new CurrencyExchangeEvent(this);
        }

    }

}
