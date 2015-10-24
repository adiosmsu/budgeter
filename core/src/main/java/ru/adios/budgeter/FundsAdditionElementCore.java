package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Date: 6/13/15
 * Time: 3:59 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsAdditionElementCore implements MoneySettable, Submitter<Treasury.BalanceAccount> {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_AMOUNT_UNIT = "amountUnit";
    public static final String FIELD_AMOUNT_DECIMAL = "amountDecimal";

    private static final Logger logger = LoggerFactory.getLogger(FundsMutationElementCore.class);
    private static final String CURRENCIES_DONT_MATCH_PRE = "Currencies don't match for";


    private final Treasury treasury;

    private final MoneyPositiveWrapper amountWrapper = new MoneyPositiveWrapper("funds addition amount");
    private Optional<Treasury.BalanceAccount> accountRef = Optional.empty();

    private boolean lockOn = false;
    private Result<Treasury.BalanceAccount> storedResult;

    public FundsAdditionElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Override
    public Money getAmount() {
        return amountWrapper.getAmount();
    }

    @Override
    public void setAmount(Money amount) {
        if (lockOn) return;
        amountWrapper.setAmount(amount);
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        if (lockOn) return;
        amountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        if (lockOn) return;
        amountWrapper.setAmountUnit(code);
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        if (lockOn) return;
        amountWrapper.setAmountUnit(unit);
    }

    public void setAccount(Treasury.BalanceAccount account) {
        if (lockOn) return;
        if (account == null) {
            accountRef = Optional.empty();
            setAmountUnit((String) null);
        } else {
            this.accountRef = Optional.of(account);
            setAmountUnit(account.getUnit());
        }
    }

    @PotentiallyBlocking
    public boolean setAccount(String accountName) {
        if (lockOn) return false;

        if (accountName == null) {
            setAccount((Treasury.BalanceAccount) null);
            return true;
        }

        final Optional<Treasury.BalanceAccount> accountForName = treasury.getAccountForName(accountName);
        if (accountForName.isPresent()) {
            setAccount(accountForName.get());
            return true;
        }

        return false;
    }

    @Nullable
    public Treasury.BalanceAccount getAccount() {
        return accountRef.orElse(null);
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return amountWrapper.getAmountDecimal();
    }

    @Nullable
    @Override
    public CurrencyUnit getAmountUnit() {
        return amountWrapper.getAmountUnit();
    }

    @PotentiallyBlocking
    @Override
    public Result<Treasury.BalanceAccount> submit() {
        final ResultBuilder<Treasury.BalanceAccount> resultBuilder = new ResultBuilder<>();
        resultBuilder.addFieldErrorIfAbsent(accountRef, FIELD_ACCOUNT);

        if (!amountWrapper.isUnitSet()) {
            resultBuilder
                    .addFieldError(FIELD_AMOUNT_UNIT)
                    .addFieldError(FIELD_AMOUNT);
        } else if (accountRef.isPresent() && !accountRef.get().getUnit().equals(amountWrapper.getAmountUnit())) {
            resultBuilder.addFieldError(FIELD_AMOUNT_UNIT, CURRENCIES_DONT_MATCH_PRE)
                    .addFieldError(FIELD_ACCOUNT, CURRENCIES_DONT_MATCH_PRE);
        }
        if (!amountWrapper.isAmountSet()) {
            resultBuilder
                    .addFieldError(FIELD_AMOUNT_DECIMAL)
                    .addFieldError(FIELD_AMOUNT);
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        final Treasury.BalanceAccount account = accountRef.get();
        try {
            treasury.addAmount(getAmount(), account.name);

            return Result.success(treasury.getAccountForName(account.name).get());
        } catch (RuntimeException ex) {
            final String mes = "Error while adding amount to " + account;
            logger.error(mes, ex);
            return resultBuilder
                    .setGeneralError(mes + ": " + ex.getMessage())
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

    @Override
    public Result<Treasury.BalanceAccount> getStoredResult() {
        return storedResult;
    }

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
