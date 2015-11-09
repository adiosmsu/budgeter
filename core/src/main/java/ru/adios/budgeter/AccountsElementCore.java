package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;

import javax.annotation.Nonnull;
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
public class AccountsElementCore implements Submitter<BalanceAccount> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_UNIT = "unit";

    private static final Logger logger = LoggerFactory.getLogger(AccountsElementCore.class);


    private final Treasury treasury;
    private final SubmitHelper<BalanceAccount> helper = new SubmitHelper<>(logger, "Error while registering balance account");

    private Optional<String> nameOpt = Optional.empty();
    private Optional<String> descOpt = Optional.empty();
    private Optional<CurrencyUnit> unitOpt = Optional.empty();

    private boolean lockOn = false;
    private Result<BalanceAccount> storedResult;

    public AccountsElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Nullable
    public String getName() {
        return nameOpt.orElse(null);
    }

    @Nullable
    public String getDescription() {
        return descOpt.orElse(null);
    }

    public void setName(String name) {
        if (lockOn) return;
        this.nameOpt = Optional.ofNullable(name);
    }

    public void setDescription(String description) {
        if (lockOn) return;
        this.descOpt = Optional.ofNullable(description);
    }

    @Nullable
    public CurrencyUnit getUnit() {
        return unitOpt.orElse(null);
    }

    public void setUnit(CurrencyUnit unit) {
        if (lockOn) return;
        this.unitOpt = Optional.ofNullable(unit);
    }

    @PotentiallyBlocking
    public Stream<BalanceAccount> streamAccountBalances() {
        return treasury.streamRegisteredAccounts();
    }

    @PotentiallyBlocking
    @Override
    public Result<BalanceAccount> submit() {
        final ResultBuilder<BalanceAccount> resultBuilder = new ResultBuilder<>();
        resultBuilder.addFieldErrorIfAbsent(nameOpt, FIELD_NAME)
                .addFieldErrorIfAbsent(unitOpt, FIELD_UNIT);

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        return helper.doSubmit(this::doSubmit, resultBuilder);
    }

    @Nonnull
    private Result<BalanceAccount> doSubmit() {
        final String name = nameOpt.get();
        treasury.registerBalanceAccount(new BalanceAccount(name, unitOpt.get(), descOpt.orElse(null)));

        return Result.success(treasury.getAccountForName(name).get());
    }

    @Override
    public void setTransactional(TransactionalSupport transactional) {
        helper.setTransactionalSupport(transactional);
    }

    @Override
    public TransactionalSupport getTransactional() {
        return helper.getTransactionalSupport();
    }

    @Override
    public void lock() {
        lockOn = true;
    }

    @Override
    public void unlock() {
        lockOn = false;
    }

    @Override
    public Result<BalanceAccount> getStoredResult() {
        return storedResult;
    }

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
