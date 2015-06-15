package ru.adios.budgeter.api;

import org.joda.money.Money;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkArgument;

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
        checkArgument(bought != null && sold != null && rate != null, "Bad data, possibly uninitialized");
    }

    public final Money sold;
    public final Money bought;
    public final BigDecimal rate;

    @NotThreadSafe
    public static final class Builder {

        private Money sold;
        private Money bought;
        private BigDecimal rate;

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

        public CurrencyExchangeEvent build() {
            return new CurrencyExchangeEvent(this);
        }

    }

}
