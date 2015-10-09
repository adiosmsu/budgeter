package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 8/14/15
 * Time: 4:58 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class AccountsElementCore implements Submitter {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_UNIT = "unit";

    private static final Logger logger = LoggerFactory.getLogger(AccountsElementCore.class);


    private final Treasury treasury;

    private Optional<String> nameOpt = Optional.empty();
    private Optional<CurrencyUnit> unitOpt = Optional.empty();

    public AccountsElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Nullable
    public String getName() {
        return nameOpt.orElse(null);
    }

    public void setName(String name) {
        this.nameOpt = Optional.of(name);
    }

    @Nullable
    public CurrencyUnit getUnit() {
        return unitOpt.orElse(null);
    }

    public void setUnit(CurrencyUnit unit) {
        this.unitOpt = Optional.of(unit);
    }

    public Stream<Treasury.BalanceAccount> streamAccountBalances() {
        return treasury.streamRegisteredAccounts();
    }

    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(nameOpt, FIELD_NAME)
                .addFieldErrorIfAbsent(unitOpt, FIELD_UNIT);

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        try {
            treasury.registerBalanceAccount(new Treasury.BalanceAccount(nameOpt.get(), unitOpt.get()));
        } catch (RuntimeException ex) {
            logger.error("Error while registering balance account", ex);
            return resultBuilder
                    .setGeneralError("Error while registering balance account: " + ex.getMessage())
                    .build();
        }

        return Result.SUCCESS;
    }

}
