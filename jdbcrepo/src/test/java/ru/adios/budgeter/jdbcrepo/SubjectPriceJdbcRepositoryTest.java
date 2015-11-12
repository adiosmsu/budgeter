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
import ru.adios.budgeter.api.SubjectPriceRepoTester;

/**
 * Date: 11/9/15
 * Time: 2:37 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPriceJdbcRepositoryTest {

    private final SubjectPriceRepoTester tester = new SubjectPriceRepoTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testCountByAgent() throws Exception {
        TestContext.ex(tester::testCountByAgent);
    }

    @Test
    public void testStream() throws Exception {
        TestContext.ex(tester::testStream);
    }

    @Test
    public void testStreamByAgent() throws Exception {
        TestContext.ex(tester::testStreamByAgent);
    }

    @Test
    public void testRegister() throws Exception {
        TestContext.ex(tester::testRegister);
    }

    @Test
    public void testPriceExists() throws Exception {
        TestContext.ex(tester::testPriceExists);
    }

    @Test
    public void testCount() throws Exception {
        TestContext.ex(tester::testCount);
    }

}
