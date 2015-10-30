package ru.adios.budgeter.inmemrepo;

import org.junit.Test;
import ru.adios.budgeter.api.AccounterTester;

/**
 * Date: 6/15/15
 * Time: 6:18 PM
 *
 * @author Mikhail Kulikov
 */
public class InnerMemoryAccounterTest {

    private final AccounterTester tester = new AccounterTester(Schema.INSTANCE);

    @Test
    public void testStreamAllPostponingReasons() throws Exception {
        tester.testStreamAllPostponingReasons();
    }

}