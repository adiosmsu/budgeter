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

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationSubjectRepoTester;

/**
 * Date: 6/15/15
 * Time: 5:28 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectPseudoTableTest {

    private final FundsMutationSubjectRepoTester tester = new FundsMutationSubjectRepoTester(Schema.INSTANCE);

    @Test
    public void testAddSubject() throws Exception {
        tester.testAddSubject();
    }

    @Test
    public void testDescription() throws Exception {
        tester.testDescription();
    }

    @Test
    public void testFindByParent() throws Exception {
        tester.testFindByParent();
    }

    @Test
    public void testFindById() throws Exception {
        tester.testFindById();
    }

    @Test
    public void testFindByName() throws Exception {
        tester.testFindByName();
    }

    @Test
    public void testUpdateChildFlag() throws Exception {
        tester.testUpdateChildFlag();
    }

    @Test
    public void testSearchByString() throws Exception {
        tester.testSearchByString();
    }

    @Test
    public void testRawAddition() throws Exception {
        tester.testRawAddition();
    }

}