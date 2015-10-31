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
        final TestCheckedRunnable r = tester::testRegisterBenefit;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

    @Test
    public void testRegisterLoss() throws Exception {
        final TestCheckedRunnable r = tester::testRegisterLoss;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

    @Test
    public void testStream() throws Exception {
        final TestCheckedRunnable r = tester::testStream;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

    @Test
    public void testStreamForDay() throws Exception {
        final TestCheckedRunnable r = tester::testStreamForDay;
        TestContext.BUNDLE.tryExecuteInTransaction(r);
    }

}