package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.AccounterTester;

/**
 * Date: 6/15/15
 * Time: 6:18 PM
 *
 * @author Mikhail Kulikov
 */
public class JdbcAccounterTest {

    private final AccounterTester tester = new AccounterTester(TestContext.BUNDLE);

    @Test
    public void testStreamAllPostponingReasons() throws Exception {
        TestContext.ex(tester::testStreamAllPostponingReasons);
    }

    @Test
    public void testStreamAllPostponingReasonsEmpty() throws Exception {
        TestContext.ex(tester::testStreamAllPostponingReasonsEmpty);
    }

    @Test
    public void testStreamAllPostponingReasonsCompat() throws Exception {
        TestContext.ex(tester::testStreamAllPostponingReasonsCompat);
    }

    @Test
    public void testStreamAllPostponingReasonsEmptyCompat() throws Exception {
        TestContext.ex(tester::testStreamAllPostponingReasonsEmptyCompat);
    }

}