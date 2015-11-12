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

package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 1:19 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventPseudoTableTest {

    private FundsMutationEventRepoTester tester = new FundsMutationEventRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterBenefit() throws Exception {
        tester.testRegisterBenefit();
    }

    @Test
    public void testRegisterLoss() throws Exception {
        tester.testRegisterLoss();
    }

    @Test
    public void testCount() throws Exception {
        tester.testCount();
    }

    @Test
    public void testStream() throws Exception {
        tester.testStream();
    }

    @Test
    public void testStreamForDay() throws Exception {
        tester.testStreamForDay();
    }

}