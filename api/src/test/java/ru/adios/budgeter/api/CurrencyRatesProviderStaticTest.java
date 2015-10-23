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
        final BigDecimal usdToEur = CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(rubToUsd, rubToEur);
        assertEquals("Wrong rate", new BigDecimal("0.933333333333333333333333"), usdToEur);
    }

}