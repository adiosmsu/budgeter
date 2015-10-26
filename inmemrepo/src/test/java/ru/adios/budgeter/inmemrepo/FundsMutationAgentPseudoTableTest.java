package ru.adios.budgeter.inmemrepo;

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationAgentRepoTester;

/**
 * Date: 7/1/15
 * Time: 6:55 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationAgentPseudoTableTest {

    private FundsMutationAgentRepoTester tester = new FundsMutationAgentRepoTester(Schema.INSTANCE);

    @Test
    public void testAddAgent() throws Exception {
        tester.testAddAgent();
    }

}