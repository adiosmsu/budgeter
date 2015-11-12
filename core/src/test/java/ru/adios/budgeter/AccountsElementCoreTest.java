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
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.inmemrepo.Schema;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Date: 10/8/15
 * Time: 9:26 PM
 *
 * @author Mikhail Kulikov
 */
public class AccountsElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) {
        caseName += ": ";
        bundle.clearSchema();

        AccountsElementCore accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setName(null);
        accountsElementCore.setName("Ha-ha");
        accountsElementCore.setUnit(Units.RUB);
        Submitter.Result<BalanceAccount> submit = accountsElementCore.submit();
        assertTrue(caseName + "No submit success of ha-ha", submit.isSuccessful());

        final Optional<Money> haBalance = bundle.treasury().accountBalance("Ha-ha");
        assertTrue(caseName + "No account for ha-ha", haBalance.isPresent());
        assertEquals(caseName + "Money not zero for ha-ha", Money.zero(Units.RUB), haBalance.get());

        accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setName("Ho-ho");
        submit = accountsElementCore.submit();
        assertFalse(caseName + "Submit unrightfully successful for ho-ho", submit.isSuccessful());
        assertEquals(caseName + "Field error not unit", AccountsElementCore.FIELD_UNIT, submit.fieldErrors.get(0).fieldInFault);

        accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setUnit(CurrencyUnit.USD);
        submit = accountsElementCore.submit();
        assertFalse(caseName + "Submit unrightfully successful for no-name", submit.isSuccessful());
        assertEquals(caseName + "Field error not name", AccountsElementCore.FIELD_NAME, submit.fieldErrors.get(0).fieldInFault);
    }

}