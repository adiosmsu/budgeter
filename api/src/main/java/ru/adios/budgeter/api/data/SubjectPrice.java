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
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 11/9/15
 * Time: 9:57 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public class SubjectPrice {

    public static Builder builder() {
        return new Builder();
    }

    public final UtcDay day;
    public final Money price;
    public final FundsMutationSubject subject;
    public final FundsMutationAgent agent;

    private SubjectPrice(Builder builder) {
        day = builder.day;
        price = builder.price;
        subject = builder.subject;
        agent = builder.agent;
        checkArgument(day != null && price != null && subject != null && agent != null, "Bad data, possibly uninitialized %s", builder);
        checkArgument(price.isPositive(), "Price must be positive");
    }

    @Override
    public String toString() {
        return "SubjectPrice{" +
                "agent=" + agent +
                ", day=" + day +
                ", price=" + price +
                ", subject=" + subject +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubjectPrice that = (SubjectPrice) o;

        return day.equals(that.day)
                && price.equals(that.price)
                && subject.equals(that.subject)
                && agent.equals(that.agent);
    }

    @Override
    public int hashCode() {
        int result = day.hashCode();
        result = 31 * result + price.hashCode();
        result = 31 * result + subject.hashCode();
        result = 31 * result + agent.hashCode();
        return result;
    }

    @NotThreadSafe
    public static final class Builder {

        private UtcDay day = new UtcDay();
        private Money price;
        private FundsMutationSubject subject;
        private FundsMutationAgent agent;

        private Builder() {}

        public Builder setSubjectPrice(SubjectPrice subjectPrice) {
            day = subjectPrice.day;
            price = subjectPrice.price;
            subject = subjectPrice.subject;
            agent = subjectPrice.agent;
            return this;
        }

        public Builder setAgent(FundsMutationAgent agent) {
            this.agent = agent;
            return this;
        }

        public Builder setPrice(Money price) {
            this.price = price;
            return this;
        }

        public Builder setSubject(FundsMutationSubject subject) {
            this.subject = subject;
            return this;
        }

        public Builder setDay(UtcDay timestamp) {
            this.day = timestamp;
            return this;
        }

        @Nullable
        public FundsMutationAgent getAgent() {
            return agent;
        }

        @Nullable
        public Money getPrice() {
            return price;
        }

        @Nullable
        public FundsMutationSubject getSubject() {
            return subject;
        }

        @Nullable
        public UtcDay getDay() {
            return day;
        }

        @Override
        public String toString() {
            return "SubjectPrice.Builder{" +
                    "agent=" + agent +
                    ", day=" + day +
                    ", price=" + price +
                    ", subject=" + subject +
                    '}';
        }

        public SubjectPrice build() {
            return new SubjectPrice(this);
        }

    }

}
