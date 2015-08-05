package ru.adios.budgeter.inmemrepo;

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationAgent;

import static org.junit.Assert.assertEquals;

/**
 * Date: 7/1/15
 * Time: 6:55 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationAgentPseudoTableTest {

    @Test
    public void testAddAgent() throws Exception {
        Schema.FUNDS_MUTATION_AGENTS.clear();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(agent);
        assertEquals("Test", Schema.FUNDS_MUTATION_AGENTS.innerTable().values().iterator().next().obj.name);
        assertEquals(agent, Schema.FUNDS_MUTATION_AGENTS.findByName("Test").get());
    }

}