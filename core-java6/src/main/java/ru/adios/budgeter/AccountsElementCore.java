package ru.adios.budgeter;

import java8.util.stream.Stream;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 9/23/15
 * Time: 8:13 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class AccountsElementCore {

    private final Treasury treasury;

    public AccountsElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    public Stream<Treasury.BalanceAccount> streamAccountBalances() {
        return treasury.streamRegisteredAccounts();
    }

}
