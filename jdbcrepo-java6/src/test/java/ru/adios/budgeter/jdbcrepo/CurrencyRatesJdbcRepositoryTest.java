package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.CurrencyRatesTester;

/**
 * Date: 6/15/15
 * Time: 8:14 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesJdbcRepositoryTest {

    private final CurrencyRatesTester tester = new CurrencyRatesTester(TestContext.BUNDLE);

    @Test
    public void testAddRate() throws Exception {
        tester.testAddRate(2000);
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testGetConversionMultiplier();
            }
        });
    }

    @Test
    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testGetLatestOptionalConversionMultiplier();
            }
        });
    }

    @Test
    public void testIsRateStale() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testIsRateStale();
            }
        });
    }

    @Test
    public void testGetLatestConversionMultiplier() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testGetLatestConversionMultiplier();
            }
        });
    }

    @Test
    public void testStreamConversionPairs() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamConversionPairs();
            }
        });
    }

}
