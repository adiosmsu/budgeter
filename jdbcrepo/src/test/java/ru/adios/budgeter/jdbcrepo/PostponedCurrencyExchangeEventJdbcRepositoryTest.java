package ru.adios.budgeter.jdbcrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 6:30 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventJdbcRepositoryTest {

    private final PostponedCurrencyExchangeEventRepoTester tester = new PostponedCurrencyExchangeEventRepoTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRememberPostponedExchange() throws Exception {
        TestContext.ex(tester::testRememberPostponedExchange);
    }

    @Test
    public void testStreamRememberedExchanges() throws Exception {
        TestContext.ex(tester::testStreamRememberedExchanges);
    }

}