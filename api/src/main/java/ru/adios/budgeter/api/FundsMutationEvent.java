package ru.adios.budgeter.api;

import org.joda.money.Money;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 6/13/15
 * Time: 3:10 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class FundsMutationEvent {

    public static Builder builder() {
        return new Builder();
    }

    public final Money amount;
    public final int quantity;
    public final FundsMutationSubject subject;

    private FundsMutationEvent(Builder builder) {
        this.amount = builder.amount;
        this.quantity = builder.quantity;
        this.subject = builder.subject;
        checkArgument(amount != null && subject != null && quantity > 0, "Bad data, possibly uninitialized");
    }

    @NotThreadSafe
    public static final class Builder {

        private Money amount;
        private int quantity;
        private FundsMutationSubject subject;

        private Builder() {
        }

        public Builder setFundsMutationEvent(FundsMutationEvent fundsMutationEvent) {
            amount = fundsMutationEvent.amount;
            quantity = fundsMutationEvent.quantity;
            subject = fundsMutationEvent.subject;
            return this;
        }

        public Builder setAmount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setSubject(FundsMutationSubject subject) {
            this.subject = subject;
            return this;
        }

        public Money getAmount() {
            return amount;
        }

        public int getQuantity() {
            return quantity;
        }

        public FundsMutationSubject getSubject() {
            return subject;
        }

        public FundsMutationEvent build() {
            return new FundsMutationEvent(this);
        }

    }

}
