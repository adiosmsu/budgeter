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

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.inmemrepo.Schema;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Date: 1/28/16
 * Time: 6:32 PM
 *
 * @author Mikhail Kulikov
 */
public class BalancesTransferCoreTest {

    @Test
    public void testInvariants() throws Exception {
        testInvariantsWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testInvariantsWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testInvariantsWith(Bundle bundle, String caseName) throws Exception {
        bundle.clearSchema();
        final BalanceAccount usdAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount eurAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final BalanceAccount rubAccount = TestUtils.prepareBalance(bundle, Units.RUB);
        final Treasury treasury = bundle.treasury();
        final BalancesTransferCore core = new BalancesTransferCore(treasury);

        core.setAmountUnit(Units.RUB);
        core.setReceiverAccount(usdAccount);

        assertEquals("Set account after setAmountUnit failed to change currency type", CurrencyUnit.USD, core.getAmountUnit());

        core.setAmount(Money.of(CurrencyUnit.EUR, 1));

        assertNull("setAmount after account set failed to nullify receiver", core.getReceiverAccount());

        core.setSenderAccount(rubAccount);

        assertEquals("Set account after setAmount failed to change currency type", Units.RUB, core.getAmountUnit());
        assertNotNull("Sender wasn't actually set", core.getSenderAccount());

        core.setReceiverAccount(eurAccount);

        assertNotNull("Receiver wasn't actually set", core.getReceiverAccount());
        assertNull("Set receiver after sender failed to nullify sender", core.getSenderAccount());
        assertEquals("Set receiver after sender failed to change currency type", CurrencyUnit.EUR, core.getAmountUnit());

        core.setAmountUnit("RUB");

        assertNull("setAmountUnit(String) after account set failed to nullify receiver", core.getReceiverAccount());
    }

    @Test
    public void testSuggestAppropriateAccounts() throws Exception {
        testSuggestAppropriateAccountsWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSuggestAppropriateAccountsWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSuggestAppropriateAccountsWith(Bundle bundle, String caseName) throws Exception {
        bundle.clearSchema();
        final BalanceAccount acc1 = TestUtils.prepareBalance(bundle, Units.RUB, "1");
        final BalanceAccount acc2 = TestUtils.prepareBalance(bundle, Units.RUB, "2");
        final BalanceAccount acc3 = TestUtils.prepareBalance(bundle, Units.RUB, "3");
        final BalanceAccount usdAccount1 = TestUtils.prepareBalance(bundle, CurrencyUnit.USD, "1");
        final BalanceAccount usdAccount2 = TestUtils.prepareBalance(bundle, CurrencyUnit.USD, "2");
        final Treasury treasury = bundle.treasury();
        final BalancesTransferCore core = new BalancesTransferCore(treasury);

        core.setReceiverAccount(acc1);
        List<BalanceAccount> suggested = suggestAndSort(core);
        assertArrayEquals("Used ruble account check failed", new BalanceAccount[] {acc2, acc3}, suggested.toArray(new BalanceAccount[2]));

        core.setSenderAccount(usdAccount1);
        suggested = suggestAndSort(core);
        assertArrayEquals("Used usd account check failed", new BalanceAccount[] {usdAccount2}, suggested.toArray(new BalanceAccount[1]));

        core.setAmountUnit(Units.RUB);
        suggested = suggestAndSort(core);
        assertArrayEquals("Unused ruble account check failed", new BalanceAccount[] {acc1, acc2, acc3}, suggested.toArray(new BalanceAccount[3]));
    }

    @Nonnull
    private List<BalanceAccount> suggestAndSort(BalancesTransferCore core) {
        final List<BalanceAccount> suggested = core.suggestAppropriateAccounts();
        Collections.sort(suggested, (o1, o2) -> o1.name.compareTo(o2.name));
        return suggested;
    }

    @Test
    public void testSubmit() throws Exception {
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        bundle.clearSchema();
        final BalanceAccount acc1 = TestUtils.prepareBalance(bundle, Units.RUB, "1");
        final BalanceAccount acc2 = TestUtils.prepareBalance(bundle, Units.RUB, "2");
        final Treasury treasury = bundle.treasury();
        final BalancesTransferCore core = new BalancesTransferCore(treasury);

        treasury.addAmount(Money.of(Units.RUB, 1000), acc1);

        core.setAmount(500, 0);
        core.setSenderAccount(acc1);
        core.setReceiverAccount(acc2);

        final Submitter.Result<BalancesTransferCore.AccountsPair> result = core.submit();

        assertTrue("Result with errors", result.isSuccessful());
        assertNotNull(result.submitResult);
        assertEquals(acc1.name, result.submitResult.sender.name);
        assertEquals(acc2.name, result.submitResult.receiver.name);
        assertEquals("Sender balance wrong", Money.of(Units.RUB, 500), result.submitResult.sender.getBalance().get());
        assertEquals("Receiver balance wrong", Money.of(Units.RUB, 500), result.submitResult.receiver.getBalance().get());
    }

}