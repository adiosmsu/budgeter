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

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 11/3/15
 * Time: 11:00 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class BalanceAccount {

    public final String name;
    public final Optional<String> description;
    public final Optional<Long> id;
    private final Optional<CurrencyUnit> unit;
    private final Optional<Money> balance;

    public BalanceAccount(@Nonnull String name, @Nonnull CurrencyUnit unit, @Nullable String description) {
        this.name = name;
        this.description = Optional.ofNullable(description);
        this.unit = Optional.of(unit);
        this.balance = Optional.empty();
        this.id = Optional.empty();
    }

    public BalanceAccount(@Nonnull Long id, @Nonnull String name, @Nullable String description, @Nonnull Money balance) {
        this.name = name;
        this.description = Optional.ofNullable(description);
        this.balance = Optional.of(balance);
        this.unit = Optional.empty();
        this.id = Optional.of(id);
    }

    public CurrencyUnit getUnit() {
        if (unit.isPresent()) {
            return unit.get();
        } else if (balance.isPresent()) {
            return balance.get().getCurrencyUnit();
        } else {
            throw new IllegalStateException("Both unit and balance are NULL");
        }
    }

    public Optional<Money> getBalance() {
        return balance;
    }

    public BigDecimal getAmount() {
        return balance.isPresent() ? balance.get().getAmount() : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BalanceAccount that = (BalanceAccount) o;

        return name.equals(that.name)
                && unit.equals(that.unit)
                && balance.equals(that.balance);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + unit.hashCode();
        result = 31 * result + balance.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(100);

        builder.append("Treasury.BalanceAccount{name=").append(name);
        if (description.isPresent()) {
            builder.append(", description=").append(description.get());
        }
        builder.append(", currency=").append(getUnit());
        if (balance.isPresent()) {
            builder.append(", balance=").append(balance.get().getAmount());
        }
        return builder.append('}')
                .toString();
    }

}
