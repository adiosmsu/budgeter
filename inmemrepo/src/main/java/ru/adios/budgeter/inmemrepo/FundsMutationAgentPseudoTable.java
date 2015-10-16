package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 7/1/15
 * Time: 6:49 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationAgentPseudoTable extends AbstractPseudoTable<Stored<FundsMutationAgent>, FundsMutationAgent> implements FundsMutationAgentRepository {

    public static final FundsMutationAgentPseudoTable INSTANCE = new FundsMutationAgentPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<FundsMutationAgent>> table = new ConcurrentHashMap<>(100, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<>(100, 0.75f, 4);

    private FundsMutationAgentPseudoTable() {}

    @Override
    public FundsMutationAgent addAgent(FundsMutationAgent agent) {
        final int id = idSequence.incrementAndGet();
        checkState(nameUniqueIndex.computeIfAbsent(agent.name, key -> {
            table.put(id, new Stored<>(id, agent));
            return id;
        }).equals(id), "Not unique name %s", agent.name);
        return agent;
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        final Integer id = nameUniqueIndex.get(name);
        return id == null
                ? Optional.empty()
                : Optional.of(table.get(id).obj);
    }

    @Override
    public void clear() {
        table.clear();
        nameUniqueIndex.clear();
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<FundsMutationAgent>> innerTable() {
        return table;
    }

}
