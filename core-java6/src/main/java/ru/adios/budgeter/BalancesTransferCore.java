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
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.util.List;

/**
 * Date: 1/26/16
 * Time: 7:27 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class BalancesTransferCore implements MoneySettable, Submitter<BalancesTransferCore.AccountsPair> {

    public static final String FIELD_SENDER_ACCOUNT = "sender account";
    public static final String FIELD_RECEIVER_ACCOUNT = "receiver account";
    public static final String FIELD_TRANSFER_AMOUNT = "transfer amount";
    public static final String FIELD_TRANSFER_AMOUNT_UNIT = "transfer amount unit";
    public static final String FIELD_TRANSFER_AMOUNT_DECIMAL = "transfer amount value";

    public static final class AccountsPair {
        public final BalanceAccount sender;
        public final BalanceAccount receiver;

        AccountsPair(BalanceAccount sender, BalanceAccount receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BalancesTransferCore.class);


    private final Treasury treasury;
    private final SubmitHelper<AccountsPair> helper = new SubmitHelper<AccountsPair>(logger, "Error while performing balances transfer business logic");

    private final MoneyPositiveWrapper trAmountWrapper = new MoneyPositiveWrapper("transfer amount");

    private Optional<BalanceAccount> senderAccountRef = Optional.empty();
    private Optional<BalanceAccount> receiverAccountRef = Optional.empty();

    private boolean lockOn = false;
    private Submitter.Result<AccountsPair> storedResult;

    public BalancesTransferCore(Treasury treasury) {
        this.treasury = treasury;
    }

    @Override
    public void setAmountDecimal(BigDecimal amountDecimal) {
        if (lockOn) return;
        trAmountWrapper.setAmountDecimal(amountDecimal);
    }

    @Override
    public void setAmountUnit(String code) {
        if (lockOn) return;
        trAmountWrapper.setAmountUnit(code);
        applyAmountUnitSideEffects(trAmountWrapper.getAmountUnit());
    }

    @Override
    public void setAmountUnit(CurrencyUnit unit) {
        if (lockOn) return;
        trAmountWrapper.setAmountUnit(unit);
        applyAmountUnitSideEffects(unit);
    }

    @Override
    public void setAmount(Money amount) {
        if (lockOn) return;
        trAmountWrapper.setAmount(amount);
        applyAmountUnitSideEffects(amount.getCurrencyUnit());
    }

    private void applyAmountUnitSideEffects(CurrencyUnit unit) {
        if (isAccountOfOtherUnit(senderAccountRef, unit)) {
            senderAccountRef = Optional.empty();
        }
        if (isAccountOfOtherUnit(receiverAccountRef, unit)) {
            receiverAccountRef = Optional.empty();
        }
    }

    private static boolean isAccountOfOtherUnit(Optional<BalanceAccount> accountRef, CurrencyUnit unit) {
        return accountRef.isPresent() && !accountRef.get().getUnit().equals(unit);
    }

    @Override
    public BigDecimal getAmountDecimal() {
        return trAmountWrapper.getAmountDecimal();
    }

    @Override
    public Money getAmount() {
        return trAmountWrapper.getAmount();
    }

    @Override
    @Nullable
    public CurrencyUnit getAmountUnit() {
        return trAmountWrapper.getAmountUnit();
    }

    @Override
    public void setAmount(int coins, int cents) {
        if (lockOn) return;
        trAmountWrapper.setAmount(coins, cents);
    }

    public void setSenderAccount(BalanceAccount senderAccount) {
        if (lockOn) return;
        senderAccountRef = helpAccountSetting(senderAccount);
    }

    public void setReceiverAccount(BalanceAccount receiverAccount) {
        if (lockOn) return;
        receiverAccountRef = helpAccountSetting(receiverAccount);
    }

    @Nullable
    public BalanceAccount getSenderAccount() {
        return senderAccountRef.orElse(null);
    }

    @Nullable
    public BalanceAccount getReceiverAccount() {
        return receiverAccountRef.orElse(null);
    }

    private Optional<BalanceAccount> helpAccountSetting(BalanceAccount account) {
        if (account == null) {
            setAmountUnit((String) null);
            return Optional.empty();
        } else {
            setAmountUnit(account.getUnit());
            return Optional.of(account);
        }
    }

    @PotentiallyBlocking
    public List<BalanceAccount> suggestAppropriateAccounts() {
        return getSuggestedAccountsStream()
                .collect(Collectors.<BalanceAccount>toList());
    }

    @PotentiallyBlocking
    public Stream<BalanceAccount> getSuggestedAccountsStream() {
        final BalanceAccount[] usedAccounts = new BalanceAccount[2];
        final CurrencyUnit curUnit;
        if (trAmountWrapper.isUnitSet()) {
            curUnit = trAmountWrapper.getAmountUnit();
        } else {
            final BalanceAccount nonEmptyAcc = senderAccountRef.orElseGet(new Supplier<BalanceAccount>() {
                @Override
                public BalanceAccount get() {
                    return BalancesTransferCore.this.receiverAccountRef.orElse(null);
                }
            });
            if (nonEmptyAcc == null) {
                return RefStreams.empty();
            }
            curUnit = nonEmptyAcc.getUnit();
        }

        fillUsedAccountsInfo(usedAccounts);

        return treasury.streamAccountsByCurrency(curUnit)
                .filter(new Predicate<BalanceAccount>() {
                    @Override
                    public boolean test(BalanceAccount account) {
                        return !account.equals(usedAccounts[0]) && !account.equals(usedAccounts[1]);
                    }
                });
    }

    private void fillUsedAccountsInfo(BalanceAccount[] usedAccounts) {
        final BalanceAccount senderAccount = getSenderAccount();
        final BalanceAccount receiverAccount = getReceiverAccount();
        if (senderAccount != null) {
            usedAccounts[0] = senderAccount;
        }
        if (receiverAccount != null) {
            usedAccounts[1] = receiverAccount;
        }
    }

    @PotentiallyBlocking
    @Override
    public Result<AccountsPair> submit() {
        final ResultBuilder<AccountsPair> resultBuilder = new ResultBuilder<AccountsPair>();

        resultBuilder.addFieldErrorIfAbsent(senderAccountRef, FIELD_SENDER_ACCOUNT);
        resultBuilder.addFieldErrorIfAbsent(receiverAccountRef, FIELD_RECEIVER_ACCOUNT);

        if (!trAmountWrapper.isUnitSet()) {
            resultBuilder
                    .addFieldError(FIELD_TRANSFER_AMOUNT_UNIT)
                    .addFieldError(FIELD_TRANSFER_AMOUNT);
        }
        if (!trAmountWrapper.isAmountSet()) {
            resultBuilder
                    .addFieldError(FIELD_TRANSFER_AMOUNT_DECIMAL)
                    .addFieldError(FIELD_TRANSFER_AMOUNT);
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        return helper.doSubmit(new Supplier<Result<AccountsPair>>() {
            @Override
            public Result<AccountsPair> get() {
                return BalancesTransferCore.this.doSubmit();
            }
        }, resultBuilder);
    }

    @Nonnull
    private Result<AccountsPair> doSubmit() {
        final Money amount = getAmount();
        final BalanceAccount sender = senderAccountRef.get();
        final BalanceAccount receiver = receiverAccountRef.get();

        treasury.addAmount(amount.negated(), sender);
        treasury.addAmount(amount, receiver);

        return Result.success(new AccountsPair(
                treasury.getAccountForName(sender.name).get(),
                treasury.getAccountForName(receiver.name).get()
        ));
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
    public Result<AccountsPair> getStoredResult() {
        return storedResult;
    }

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
