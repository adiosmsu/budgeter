package ru.adios.budgeter.inmemrepo;

import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;

/**
 * Date: 9/17/15
 * Time: 5:53 PM
 *
 * @author Mikhail Kulikov
 */
public final class StoredBalanceAccount extends Stored<Money> {

    final String name;

    StoredBalanceAccount(int id, Money obj, String name) {
        super(id, obj);
        this.name = name;
    }

    Treasury.BalanceAccount createBalanceAccount() {
        return new Treasury.BalanceAccount((long) id, name, obj);
    }

    Treasury.BalanceAccount createBalanceAccount(Long id) {
        return new Treasury.BalanceAccount(id, name, obj);
    }

    @Override
    public String toString() {
        return "StoredBalanceAccount{" +
                "name='" + name + '\'' +
                ", " + super.toString() + '}';
    }

}
