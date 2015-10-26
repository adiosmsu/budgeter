package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyExchangeTester;

/**
 * Date: 6/15/15
 * Time: 1:13 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventPseudoTableTest {

    public final CurrencyExchangeTester tester = new CurrencyExchangeTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterCurrencyExchange() throws Exception {
        tester.testRegisterCurrencyExchange();
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        tester.testStreamExchangeEvents();
    }

}
