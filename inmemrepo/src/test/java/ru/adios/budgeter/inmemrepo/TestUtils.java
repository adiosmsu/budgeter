package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.FundsMutationAgent;

/**
 * Date: 7/1/15
 * Time: 7:50 AM
 *
 * @author Mikhail Kulikov
 */
public class TestUtils {

    static FundsMutationAgent prepareTestAgent() {
        final FundsMutationAgent test = FundsMutationAgent.builder().setName("Test").build();
        Schema.FUNDS_MUTATION_AGENTS.clear();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(test);
        return test;
    }

}
