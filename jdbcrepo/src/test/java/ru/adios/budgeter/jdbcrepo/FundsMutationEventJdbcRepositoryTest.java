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
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterBenefit();
            }
        });
    }

    @Test
    public void testRegisterLoss() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterLoss();
            }
        });
    }

    @Test
    public void testStream() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStream();
            }
        });
    }

}