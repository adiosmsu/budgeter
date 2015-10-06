package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;

import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 3:59 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsAdditionElementCore implements MoneySettable, Submitter {

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

    @Override
    public void setAmount(int coins, int cents) {
        amountWrapper.setAmount(coins, cents);
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return amountWrapper.getAmountDecimal();
    }

    @Override
    public CurrencyUnit getAmountUnit() {
        return amountWrapper.getAmountUnit();
    }

    @Override
    public Result submit() {
        final ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.addFieldErrorIfAbsent(accountRef, "account");

        if (!amountWrapper.isUnitSet()) {
            resultBuilder
                    .addFieldError("amountUnit")
                    .addFieldError("amount");
        }
        if (!amountWrapper.isAmountSet()) {
            resultBuilder
                    .addFieldError("amountDecimal")
                    .addFieldError("amount");
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        treasury.addAmount(getAmount(), accountRef.get().name);

        return Result.SUCCESS;
    }

}
