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

package ru.adios.budgeter.api;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/23/15
 * Time: 10:09 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesProviderStaticTest {

    @Test
    public void testGetConversionMultiplierFromIntermediateMultipliers() throws Exception {
        final BigDecimal rubToUsd = new BigDecimal("0.015");
        final BigDecimal rubToEur = new BigDecimal("0.014");
        final BigDecimal usdToEur = CurrencyRatesProvider.Static.getConversionMultiplierFromIntermediateMultipliers(rubToUsd, rubToEur);
        assertEquals("Wrong rate", new BigDecimal("0.933333333333333333333333"), usdToEur);
    }

}