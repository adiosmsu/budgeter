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
        TestContext.ex(tester::testRegisterCurrencyExchange);
    }

    @Test
    public void testCount() throws Exception {
        TestContext.ex(tester::testCount);
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        TestContext.ex(tester::testStreamExchangeEvents);
    }

    @Test
    public void testStreamForDay() throws Exception {
        TestContext.ex(tester::testStreamForDay);
    }

}
