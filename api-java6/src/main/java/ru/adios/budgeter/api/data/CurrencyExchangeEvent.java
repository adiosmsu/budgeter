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
import org.threeten.bp.OffsetDateTime;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;

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
        this.boughtAccount = builder.boughtAccount;
        this.soldAccount = builder.soldAccount;
        this.rate = builder.rate.stripTrailingZeros();
        this.timestamp = builder.timestamp;
        this.agent = builder.agent;
        checkArgument(bought != null && sold != null && soldAccount != null && boughtAccount != null && rate != null && agent != null, "Bad data, possibly uninitialized");
    }

    public final Money sold;
    public final Money bought;
    public final BalanceAccount soldAccount;
    public final BalanceAccount boughtAccount;
    public final BigDecimal rate;
    public final OffsetDateTime timestamp;
    public final FundsMutationAgent agent;

    @Override
    public String toString() {
        return "CurrencyExchangeEvent{" +
                "sold=" + sold +
                ", bought=" + bought +
                ", soldAccount=" + soldAccount +
                ", boughtAccount=" + boughtAccount +
                ", rate=" + rate +
                ", timestamp=" + timestamp +
                ", agent=" + agent +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrencyExchangeEvent that = (CurrencyExchangeEvent) o;

        return sold.equals(that.sold) && bought.equals(that.bought)
                && soldAccount.equals(that.soldAccount)
                && boughtAccount.equals(that.boughtAccount)
                && rate.equals(that.rate)
                && timestamp.equals(that.timestamp)
                && agent.equals(that.agent);
    }

    @Override
    public int hashCode() {
        int result = sold.hashCode();
        result = 31 * result + bought.hashCode();
        result = 31 * result + soldAccount.hashCode();
        result = 31 * result + boughtAccount.hashCode();
        result = 31 * result + rate.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + agent.hashCode();
        return result;
    }

    @NotThreadSafe
    public static final class Builder {

        private Money sold;
        private Money bought;
        private BalanceAccount soldAccount;
        private BalanceAccount boughtAccount;
        private BigDecimal rate;
        private OffsetDateTime timestamp = OffsetDateTime.now();
        private FundsMutationAgent agent;

        private Builder() {}

        public Builder setEvent(CurrencyExchangeEvent event) {
            sold = event.sold;
            bought = event.bought;
            soldAccount = event.soldAccount;
            boughtAccount = event.boughtAccount;
            rate = event.rate;
            timestamp = event.timestamp;
            agent = event.agent;
            return this;
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

        public Builder setBoughtAccount(BalanceAccount boughtAccount) {
            this.boughtAccount = boughtAccount;
            return this;
        }

        public Builder setSoldAccount(BalanceAccount soldAccount) {
            this.soldAccount = soldAccount;
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

        public CurrencyExchangeEvent build() {
            return new CurrencyExchangeEvent(this);
        }

    }

}
