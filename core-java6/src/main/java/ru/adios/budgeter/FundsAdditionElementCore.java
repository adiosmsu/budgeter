/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Supplier;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 3:59 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsAdditionElementCore implements MoneySettable, Submitter<BalanceAccount> {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_AMOUNT_UNIT = "amountUnit";
    public static final String FIELD_AMOUNT_DECIMAL = "amountDecimal";

    private static final Logger logger = LoggerFactory.getLogger(FundsMutationElementCore.class);
    private static final String CURRENCIES_DONT_MATCH_PRE = "Currencies don't match for";


    private final Treasury treasury;
    private final SubmitHelper<BalanceAccount> helper = new SubmitHelper<BalanceAccount>(logger, "Error while adding amount to account");

    private final MoneyPositiveWrapper amountWrapper = new MoneyPositiveWrapper("funds addition amount");
    private Optional<BalanceAccount> accountRef = Optional.empty();

    private boolean lockOn = false;
    private Result<BalanceAccount> storedResult;

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

    public void setAccount(BalanceAccount account) {
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
            setAccount((BalanceAccount) null);
            return true;
        }

        final Optional<BalanceAccount> accountForName = treasury.getAccountForName(accountName);
        if (accountForName.isPresent()) {
            setAccount(accountForName.get());
            return true;
        }

        return false;
    }

    @Nullable
    public BalanceAccount getAccount() {
        return accountRef.orElse(null);
    }

    @Override
    public void setAmount(int coins, int cents) {
        if (lockOn) return;
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

    @PotentiallyBlocking
    @Override
    public Result<BalanceAccount> submit() {
        final ResultBuilder<BalanceAccount> resultBuilder = new ResultBuilder<BalanceAccount>();
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

        return helper.doSubmit(new Supplier<Result<BalanceAccount>>() {
            @Override
            public Result<BalanceAccount> get() {
                return doSubmit();
            }
        }, resultBuilder);
    }

    @Nonnull
    private Result<BalanceAccount> doSubmit() {
        final BalanceAccount account = accountRef.get();
        treasury.addAmount(getAmount(), account.name);

        return Result.success(treasury.getAccountForName(account.name).get());
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
