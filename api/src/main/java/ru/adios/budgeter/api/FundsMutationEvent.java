package ru.adios.budgeter.api;

import org.joda.money.Money;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.OffsetDateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
    public final OffsetDateTime timestamp;
    public final FundsMutationAgent agent;

    private FundsMutationEvent(Builder builder) {
        this.amount = builder.amount;
        this.quantity = builder.quantity;
        this.subject = builder.subject;
        this.timestamp = builder.timestamp;
        this.agent = builder.agent;
        checkArgument(amount != null && subject != null && quantity > 0 && timestamp != null && agent != null, "Bad data, possibly uninitialized");
    }

    @Override
    public String toString() {
        return "FundsMutationEvent{" +
                "amount=" + amount +
                ", quantity=" + quantity +
                ", subject=" + subject +
                ", timestamp=" + timestamp +
                ", agent=" + agent +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FundsMutationEvent that = (FundsMutationEvent) o;

        return quantity == that.quantity
                && amount.equals(that.amount)
                && subject.equals(that.subject)
                && timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = amount.hashCode();
        result = 31 * result + quantity;
        result = 31 * result + subject.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    @NotThreadSafe
    public static final class Builder {

        private Money amount;
        private int quantity;
        private FundsMutationSubject subject;
        private OffsetDateTime timestamp = OffsetDateTime.now();
        private FundsMutationAgent agent;

        private Builder() {}

        public Builder setFundsMutationEvent(FundsMutationEvent fundsMutationEvent) {
            amount = fundsMutationEvent.amount;
            quantity = fundsMutationEvent.quantity;
            subject = fundsMutationEvent.subject;
            timestamp = fundsMutationEvent.timestamp;
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

        public Builder setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = checkNotNull(timestamp);
            return this;
        }

        public Builder setAgent(FundsMutationAgent agent) {
            this.agent = agent;
            return this;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public Money getAmount() {
            return amount;
        }

        public FundsMutationAgent getAgent() {
            return agent;
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
