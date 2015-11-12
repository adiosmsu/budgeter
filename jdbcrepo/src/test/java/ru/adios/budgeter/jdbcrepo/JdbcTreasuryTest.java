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

package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.TreasuryTester;

/**
 * Date: 6/15/15
 * Time: 6:54 PM
 *
 * @author Mikhail Kulikov
 */
public class JdbcTreasuryTest {

    private final TreasuryTester tester = new TreasuryTester(TestContext.BUNDLE);

    @Test
    public void testAccountBalance() throws Exception {
        TestContext.ex(tester::testAccountBalance);
    }

    @Test
    public void testStreamRegisteredAccounts() throws Exception {
        TestContext.ex(tester::testStreamRegisteredAccounts);
    }

    @Test
    public void testAddAmount() throws Exception {
        tester.testAddAmount();
    }

    @Test
    public void testGetAccountForName() throws Exception {
        TestContext.ex(tester::testGetAccountForName);
    }

    @Test
    public void testAmount() throws Exception {
        TestContext.ex(tester::testAmount);
    }

    @Test
    public void testRegisterBalanceAccount() throws Exception {
        TestContext.ex(tester::testRegisterBalanceAccount);
    }

    @Test
    public void testGetAccountWithId() throws Exception {
        TestContext.ex(tester::testGetAccountWithId);
    }

    @Test
    public void testStreamAccountsByCurrency() throws Exception {
        TestContext.ex(tester::testStreamAccountsByCurrency);
    }

    @Test
    public void testStreamRegisteredCurrencies() throws Exception {
        TestContext.ex(tester::testStreamRegisteredCurrencies);
    }

    @Test
    public void testZeroValue() throws Exception {
        TestContext.ex(tester::testZeroValue);
    }

}