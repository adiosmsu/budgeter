package ru.adios.budgeter.jdbcrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.SubjectPriceRepoTester;

/**
 * Date: 11/9/15
 * Time: 2:37 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPriceJdbcRepositoryTest {

    private final SubjectPriceRepoTester tester = new SubjectPriceRepoTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testCountByAgent() throws Exception {
        TestContext.ex(tester::testCountByAgent);
    }

    @Test
    public void testStream() throws Exception {
        TestContext.ex(tester::testStream);
    }

    @Test
    public void testStreamByAgent() throws Exception {
        TestContext.ex(tester::testStreamByAgent);
    }

    @Test
    public void testRegister() throws Exception {
        TestContext.ex(tester::testRegister);
    }

    @Test
    public void testPriceExists() throws Exception {
        TestContext.ex(tester::testPriceExists);
    }

    @Test
    public void testCount() throws Exception {
        TestContext.ex(tester::testCount);
    }

}
