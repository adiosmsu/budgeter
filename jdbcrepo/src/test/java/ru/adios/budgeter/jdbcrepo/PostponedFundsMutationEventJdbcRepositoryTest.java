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
        TestContext.ex(tester::testRememberPostponedExchangeableBenefit);
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        TestContext.ex(tester::testRememberPostponedExchangeableLoss);
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        TestContext.ex(tester::testStreamRememberedBenefits);
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        TestContext.ex(tester::testStreamRememberedLosses);
    }

    @Test
    public void testStreamRememberedEvents() throws Exception {
        TestContext.ex(tester::testStreamRememberedEvents);
    }

}