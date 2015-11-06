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
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterBenefit();
            }
        });
    }

    @Test
    public void testRegisterLoss() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterLoss();
            }
        });
    }

    @Test
    public void testCount() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testCount();
            }
        });
    }

    @Test
    public void testStream() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStream();
            }
        });
    }

    @Test
    public void testStreamForDay() throws Exception {
        TestContext.BUNDLE.tryExecuteInTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamForDay();
            }
        });
    }

}