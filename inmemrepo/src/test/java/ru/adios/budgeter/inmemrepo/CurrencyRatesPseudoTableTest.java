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
        tester.testAddRate();
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
