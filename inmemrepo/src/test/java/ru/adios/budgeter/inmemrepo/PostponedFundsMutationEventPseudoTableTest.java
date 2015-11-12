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
import ru.adios.budgeter.api.PostponedFundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 6:43 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventPseudoTableTest {

    private final PostponedFundsMutationEventRepoTester tester = new PostponedFundsMutationEventRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRememberPostponedExchangeableBenefit() throws Exception {
        tester.testRememberPostponedExchangeableBenefit();
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        tester.testRememberPostponedExchangeableLoss();
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        tester.testStreamRememberedBenefits();
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        tester.testStreamRememberedLosses();
    }

    @Test
    public void testStreamRememberedEvents() throws Exception {
        tester.testStreamRememberedEvents();
    }

}