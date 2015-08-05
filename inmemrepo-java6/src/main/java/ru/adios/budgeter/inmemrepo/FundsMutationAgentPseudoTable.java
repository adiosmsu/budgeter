package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.Nonnull;
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

    private final ConcurrentHashMap<Integer, Stored<FundsMutationAgent>> table = new ConcurrentHashMap<Integer, Stored<FundsMutationAgent>>(100, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<String, Integer>(100, 0.75f, 4);

    private FundsMutationAgentPseudoTable() {}

    @Override
    public void addAgent(final FundsMutationAgent agent) {
        final int id = idSequence.incrementAndGet();
        checkState(nameUniqueIndex.computeIfAbsent(agent.name, new Function<String, Integer>() {
            @Override
            public Integer apply(String s) {
                table.put(id, new Stored<FundsMutationAgent>(id, agent));
                return id;
            }
        }).equals(id), "Not unique name %s", agent.name);
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        final Integer id = nameUniqueIndex.get(name);
        return id == null
                ? Optional.<FundsMutationAgent>empty()
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
