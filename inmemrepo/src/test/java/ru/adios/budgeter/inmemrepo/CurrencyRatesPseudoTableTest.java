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
import ru.adios.budgeter.api.CurrencyRatesTester;

/**
 * Date: 6/15/15
 * Time: 8:14 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesPseudoTableTest {

    private final CurrencyRatesTester tester = new CurrencyRatesTester(Schema.INSTANCE);

    @Test
    public void testAddRate() throws Exception {
        tester.testAddRate(1000);
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        tester.testGetConversionMultiplier();
    }

    @Test
    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        tester.testGetLatestOptionalConversionMultiplier();
    }

    @Test
    public void testIsRateStale() throws Exception {
        tester.testIsRateStale();
    }

    @Test
    public void testGetLatestConversionMultiplier() throws Exception {
        tester.testGetLatestConversionMultiplier();
    }

    @Test
    public void testStreamConversionPairs() throws Exception {
        tester.testStreamConversionPairs();
    }

}
