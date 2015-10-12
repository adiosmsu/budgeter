package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nullable;
import java.math.BigDecimal;

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

    private final MoneyWrapperBean amountWrapper = new MoneyWrapperBean("funds addition amount");

    private Optional<Treasury.BalanceAccount> accountRef = Optional.empty();

    public FundsAdditionElementCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Override
    public Money getAmount() {
        return amountWrapper.getAmount();
    }

    @Override
    public void setAmount(Money amount) {
        amountWrapper.setAmount(amount);
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        amountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        amountWrapper.setAmountUnit(code);
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        amountWrapper.setAmountUnit(unit);
    }

    public void setAccount(Treasury.BalanceAccount account) {
        this.accountRef = Optional.of(account);
        setAmountUnit(account.getUnit());
    }

    public void setAccount(String accountName) {
        final Optional<Treasury.BalanceAccount> accountForName = treasury.getAccountForName(accountName);
        if (accountForName.isPresent()) {
            setAccount(accountForName.get());
        }
    }

    @Nullable
    public Treasury.BalanceAccount getAccount() {
        return accountRef.orElse(null);
    }

    @Override
    public void setAmount(int coins, int cents) {
        amountWrapper.setAmount(coins, cents);
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

    @Override
    public Result<Treasury.BalanceAccount> submit() {
        final ResultBuilder<Treasury.BalanceAccount> resultBuilder = new ResultBuilder<Treasury.BalanceAccount>();
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

}
