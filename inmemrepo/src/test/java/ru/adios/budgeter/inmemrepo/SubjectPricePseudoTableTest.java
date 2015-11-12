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
import ru.adios.budgeter.api.SubjectPriceRepoTester;

/**
 * Date: 11/9/15
 * Time: 12:30 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPricePseudoTableTest {

    private final SubjectPriceRepoTester tester = new SubjectPriceRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testCountByAgent() throws Exception {
        tester.testCountByAgent();
    }

    @Test
    public void testStream() throws Exception {
        tester.testStream();
    }

    @Test
    public void testStreamByAgent() throws Exception {
        tester.testStreamByAgent();
    }

    @Test
    public void testRegister() throws Exception {
        tester.testRegister();
    }

    @Test
    public void testPriceExists() throws Exception {
        tester.testPriceExists();
    }

    @Test
    public void testCount() throws Exception {
        tester.testCount();
    }

}
