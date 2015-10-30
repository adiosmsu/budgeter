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
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterCurrencyExchange();
            }
        });
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamExchangeEvents();
            }
        });
    }

}
