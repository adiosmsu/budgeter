package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 6:30 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventPseudoTableTest {

    private final PostponedCurrencyExchangeEventRepoTester tester = new PostponedCurrencyExchangeEventRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRememberPostponedExchange() throws Exception {
        tester.testRememberPostponedExchange();
    }

    @Test
    public void testStreamRememberedExchanges() throws Exception {
        tester.testStreamRememberedExchanges();
    }

}