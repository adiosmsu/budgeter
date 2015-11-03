package ru.adios.budgeter.api.data;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Date: 11/3/15
 * Time: 10:57 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class BalanceAccount {

    public final String name;
    @Nullable
    public final Long id;
    @Nullable
    private final CurrencyUnit unit;
    @Nullable
    private final Money balance;

    @SuppressWarnings("NullableProblems")
    public BalanceAccount(@Nonnull String name, @Nonnull CurrencyUnit unit) {
        this.name = name;
        this.unit = unit;
        this.balance = null;
        this.id = null;
    }

    @SuppressWarnings("NullableProblems")
    public BalanceAccount(@Nonnull Long id, @Nonnull String name, @Nonnull Money balance) {
        this.name = name;
        this.balance = balance;
        this.unit = null;
        this.id = id;
    }

    public CurrencyUnit getUnit() {
        if (unit != null) {
            return unit;
        } else if (balance != null) {
            return balance.getCurrencyUnit();
        } else {
            throw new IllegalStateException("Both unit and balance are NULL");
        }
    }

    @Nullable
    public Money getBalance() {
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BalanceAccount that = (BalanceAccount) o;

        return name.equals(that.name)
                && !(unit != null ? !unit.equals(that.unit) : that.unit != null)
                && !(balance != null ? !balance.equals(that.balance) : that.balance != null);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        result = 31 * result + (balance != null ? balance.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(100);
        builder.append("Treasury.BalanceAccount{name=")
                .append(name)
                .append(", currency=")
                .append(getUnit());
        if (balance != null) {
            builder.append(", balance=")
                    .append(balance.getAmount());
        }
        builder.append('}');
        return builder.toString();
    }

}
