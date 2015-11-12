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

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyExchangeTester;

/**
 * Date: 6/15/15
 * Time: 1:13 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventJdbcRepositoryTest {

    public final CurrencyExchangeTester tester = new CurrencyExchangeTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterCurrencyExchange() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterCurrencyExchange();
            }
        });
    }

    @Test
    public void testCount() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testCount();
            }
        });
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamExchangeEvents();
            }
        });
    }

    @Test
    public void testStreamForDay() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamForDay();
            }
        });
    }

}
