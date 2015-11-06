package ru.adios.budgeter.jdbcrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 1:19 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventJdbcRepositoryTest {

    private FundsMutationEventRepoTester tester = new FundsMutationEventRepoTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterBenefit() throws Exception {
        TestContext.ex(tester::testRegisterBenefit);
    }

    @Test
    public void testRegisterLoss() throws Exception {
        TestContext.ex(tester::testRegisterLoss);
    }

    @Test
    public void testCount() throws Exception {
        TestContext.ex(tester::testCount);
    }

    @Test
    public void testStream() throws Exception {
        TestContext.ex(tester::testStream);
    }

    @Test
    public void testStreamForDay() throws Exception {
        TestContext.ex(tester::testStreamForDay);
    }

}