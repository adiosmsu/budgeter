package ru.adios.budgeter.jdbcrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyExchangeTester;

/**
 * Date: 6/15/15
 * Time: 1:13 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventJdbcRepositoryTest {

    public final CurrencyExchangeTester tester = new CurrencyExchangeTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterCurrencyExchange() throws Exception {
        final TestCheckedRunnable r = tester::testRegisterCurrencyExchange;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        final TestCheckedRunnable r = tester::testStreamExchangeEvents;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

    @Test
    public void testStreamForDay() throws Exception {
        final TestCheckedRunnable r = tester::testStreamForDay;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

}
