package ru.adios.budgeter;

import java8.util.Optional;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;
import ru.adios.budgeter.inmemrepo.FundsMutationAgentPseudoTable;
import ru.adios.budgeter.inmemrepo.Schema;

/**
 * Date: 7/1/15
 * Time: 6:58 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationAgentMock implements FundsMutationAgentRepository {

    private final FundsMutationAgentPseudoTable table = Schema.FUNDS_MUTATION_AGENTS;

    @Override
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        return table.addAgent(agent);
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        return table.findByName(name);
    }

}
