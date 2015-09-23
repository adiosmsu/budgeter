package ru.adios.budgeter;

import ru.adios.budgeter.api.Treasury;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.stream.Stream;

/**
 * Date: 8/14/15
 * Time: 4:58 PM
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
