package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 6:43 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventPseudoTableTest {

    private final PostponedFundsMutationEventRepoTester tester = new PostponedFundsMutationEventRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRememberPostponedExchangeableBenefit() throws Exception {
        tester.testRememberPostponedExchangeableBenefit();
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        tester.testRememberPostponedExchangeableLoss();
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        tester.testStreamRememberedBenefits();
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        tester.testStreamRememberedLosses();
    }

}