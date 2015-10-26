package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.stream.Stream;
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
    public Optional<FundsMutationAgent> getById(Long id) {
        return table.getById(id);
    }

    @Override
    public Long currentSeqValue() {
        return table.currentSeqValue();
    }

    @Override
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        return table.addAgent(agent);
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        return null;
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        return table.findByName(name);
    }

}
