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

    @Test
    public void testStreamAllPostponingReasonsEmpty() throws Exception {
        tester.testStreamAllPostponingReasonsEmpty();
    }

    @Test
    public void testStreamAllPostponingReasonsCompat() throws Exception {
        tester.testStreamAllPostponingReasonsCompat();
    }

    @Test
    public void testStreamAllPostponingReasonsEmptyCompat() throws Exception {
        tester.testStreamAllPostponingReasonsEmptyCompat();
    }

}