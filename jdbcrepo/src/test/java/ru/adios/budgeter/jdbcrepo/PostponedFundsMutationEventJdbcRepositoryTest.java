package ru.adios.budgeter.jdbcrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 6:43 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventJdbcRepositoryTest {

    private final PostponedFundsMutationEventRepoTester tester = new PostponedFundsMutationEventRepoTester(TestContext.BUNDLE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRememberPostponedExchangeableBenefit() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRememberPostponedExchangeableBenefit();
            }
        });
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRememberPostponedExchangeableLoss();
            }
        });
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamRememberedBenefits();
            }
        });
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamRememberedLosses();
            }
        });
    }

}