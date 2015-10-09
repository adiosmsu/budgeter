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
public final class FundsAdditionElementCore implements MoneySettable, Submitter {

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

    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
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

        try {
            treasury.addAmount(getAmount(), accountRef.get().name);
        } catch (RuntimeException ex) {
            final String mes = "Error while adding amount to " + accountRef.get();
            logger.error(mes, ex);
            return resultBuilder
                    .setGeneralError(mes + ": " + ex.getMessage())
                    .build();
        }

        return Result.SUCCESS;
    }

}
