/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.api.data;

import org.joda.money.Money;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

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
    public final BalanceAccount relevantBalance;
    public final BigDecimal portion;
    public final FundsMutationSubject subject;
    public final OffsetDateTime timestamp;
    public final FundsMutationAgent agent;

    private FundsMutationEvent(Builder builder) {
        this.amount = builder.amount;
        this.relevantBalance = builder.relevantBalance;
        this.subject = builder.subject;
        this.portion = builder.portion;
        this.timestamp = builder.timestamp;
        this.agent = builder.agent;
        checkArgument(
                amount != null && relevantBalance != null && subject != null && portion != null
                        && portion.compareTo(BigDecimal.ZERO) > 0 && timestamp != null && agent != null,
                "Bad data, possibly uninitialized %s",
                builder
        );
    }

    public Money getPrice() {
        return amount.dividedBy(portion, RoundingMode.HALF_DOWN);
    }

    @Override
    public String toString() {
        return "FundsMutationEvent{" +
                "amount=" + amount +
                ", relevantBalance=" + relevantBalance +
                ", portion=" + portion +
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

        return portion.equals(that.portion)
                && amount.equals(that.amount)
                && relevantBalance.equals(that.relevantBalance)
                && subject.equals(that.subject)
                && timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = amount.hashCode();
        result = 31 * result + portion.hashCode();
        result = 31 * result + relevantBalance.hashCode();
        result = 31 * result + subject.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    @NotThreadSafe
    public static final class Builder {

        private Money amount;
        private BalanceAccount relevantBalance;
        private BigDecimal portion = BigDecimal.ONE;
        private FundsMutationSubject subject;
        private OffsetDateTime timestamp = OffsetDateTime.now();
        private FundsMutationAgent agent;

        private Builder() {}

        public Builder setFundsMutationEvent(FundsMutationEvent fundsMutationEvent) {
            agent = fundsMutationEvent.agent;
            amount = fundsMutationEvent.amount;
            relevantBalance = fundsMutationEvent.relevantBalance;
            subject = fundsMutationEvent.subject;
            timestamp = fundsMutationEvent.timestamp;
            return this;
        }

        @Override
        public String toString() {
            return "FundsMutationEvent.Builder{" +
                    "amount=" + amount +
                    ", relevantBalance=" + relevantBalance +
                    ", portion=" + portion +
                    ", subject=" + subject +
                    ", timestamp=" + timestamp +
                    ", agent=" + agent +
                    '}';
        }

        public Builder setAmount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder setPortion(BigDecimal portion) {
            this.portion = portion;
            return this;
        }

        public Builder setRelevantBalance(BalanceAccount relevantBalance) {
            this.relevantBalance = relevantBalance;
            return this;
        }

        public Builder setSubject(FundsMutationSubject subject) {
            this.subject = subject;
            return this;
        }

        public Builder setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setAgent(FundsMutationAgent agent) {
            this.agent = agent;
            return this;
        }

        public BigDecimal getPortion() {
            return portion;
        }

        @Nullable
        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        @Nullable
        public Money getAmount() {
            return amount;
        }

        @Nullable
        public FundsMutationAgent getAgent() {
            return agent;
        }

        @Nullable
        public FundsMutationSubject getSubject() {
            return subject;
        }

        @Nullable
        public BalanceAccount getRelevantBalance() {
            return relevantBalance;
        }

        public FundsMutationEvent build() {
            return new FundsMutationEvent(this);
        }

    }

}
