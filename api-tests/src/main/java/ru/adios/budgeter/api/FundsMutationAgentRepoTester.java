package ru.adios.budgeter.api;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/26/15
 * Time: 4:41 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationAgentRepoTester {

    private final Bundle bundle;

    public FundsMutationAgentRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testAddAgent() throws Exception {
        bundle.clear(Bundle.Repo.FUNDS_MUTATION_AGENTS);
        final FundsMutationAgentRepository agentRepository = bundle.fundsMutationAgents();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        agentRepository.addAgent(agent);
        assertEquals("Test", agentRepository.getById(agentRepository.currentSeqValue()).get().name);
        assertEquals(agent, agentRepository.findByName("Test").get());
    }

}
