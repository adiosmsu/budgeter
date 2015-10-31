package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationEventRepoTester;

/**
 * Date: 6/15/15
 * Time: 1:19 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventPseudoTableTest {

    private FundsMutationEventRepoTester tester = new FundsMutationEventRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testRegisterBenefit() throws Exception {
        tester.testRegisterBenefit();
    }

    @Test
    public void testRegisterLoss() throws Exception {
        tester.testRegisterLoss();
    }

    @Test
    public void testStream() throws Exception {
        tester.testStream();
    }

    @Test
    public void testStreamForDay() throws Exception {
        tester.testStreamForDay();
    }

}