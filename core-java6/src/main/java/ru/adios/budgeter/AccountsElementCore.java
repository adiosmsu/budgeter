package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 9/23/15
 * Time: 8:13 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class AccountsElementCore implements Submitter<Treasury.BalanceAccount> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_UNIT = "unit";

    private static final Logger logger = LoggerFactory.getLogger(AccountsElementCore.class);


    private final Treasury treasury;

    private Optional<String> nameOpt = Optional.empty();
    private Optional<CurrencyUnit> unitOpt = Optional.empty();

    private boolean lockOn = false;

    public AccountsElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Nullable
    public String getName() {
        return nameOpt.orElse(null);
    }

    public void setName(String name) {
        if (lockOn) return;
        this.nameOpt = Optional.of(name);
    }

    @Nullable
    public CurrencyUnit getUnit() {
        return unitOpt.orElse(null);
    }

    public void setUnit(CurrencyUnit unit) {
        if (lockOn) return;
        this.unitOpt = Optional.of(unit);
    }

    public Stream<Treasury.BalanceAccount> streamAccountBalances() {
        return treasury.streamRegisteredAccounts();
    }

    @Override
    public Result<Treasury.BalanceAccount> submit() {
        final ResultBuilder<Treasury.BalanceAccount> resultBuilder = new ResultBuilder<Treasury.BalanceAccount>();
        resultBuilder.addFieldErrorIfAbsent(nameOpt, FIELD_NAME)
                .addFieldErrorIfAbsent(unitOpt, FIELD_UNIT);

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        try {
            final String name = nameOpt.get();
            treasury.registerBalanceAccount(new Treasury.BalanceAccount(name, unitOpt.get()));

            return Result.success(treasury.getAccountForName(name).get());
        } catch (RuntimeException ex) {
            logger.error("Error while registering balance account", ex);
            return resultBuilder
                    .setGeneralError("Error while registering balance account: " + ex.getMessage())
                    .build();
        }
    }

    @Override
    public void lock() {
        lockOn = true;
    }

    @Override
    public void unlock() {
        lockOn = false;
    }

}
