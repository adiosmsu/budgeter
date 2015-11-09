package ru.adios.budgeter.inmemrepo;

import org.joda.money.Money;
import ru.adios.budgeter.api.data.BalanceAccount;

/**
 * Date: 9/17/15
 * Time: 5:53 PM
 *
 * @author Mikhail Kulikov
 */
public final class StoredBalanceAccount extends Stored<Money> {

    final String name;
    final String desc;

    StoredBalanceAccount(int id, Money obj, String name, String desc) {
        super(id, obj);
        this.name = name;
        this.desc = desc;
    }

    BalanceAccount createBalanceAccount() {
        return new BalanceAccount((long) id, name, desc, obj);
    }

    BalanceAccount createBalanceAccount(Long id) {
        return new BalanceAccount(id, name, desc, obj);
    }

    @Override
    public String toString() {
        return "StoredBalanceAccount{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", " + super.toString() + '}';
    }

}
