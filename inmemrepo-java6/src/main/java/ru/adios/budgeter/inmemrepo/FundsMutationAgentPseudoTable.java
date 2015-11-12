/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.FundsMutationAgentRepository;
import ru.adios.budgeter.api.data.FundsMutationAgent;

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
    public Optional<FundsMutationAgent> getById(Long id) {
        final Stored<FundsMutationAgent> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(FundsMutationAgent.withId(stored.obj, id));
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public FundsMutationAgent addAgent(final FundsMutationAgent agent) {
        final int id = idSequence.incrementAndGet();
        checkState(nameUniqueIndex.computeIfAbsent(agent.name, new Function<String, Integer>() {
            @Override
            public Integer apply(String s) {
                table.put(id, new Stored<FundsMutationAgent>(id, FundsMutationAgent.withId(agent, id)));
                return id;
            }
        }).equals(id), "Not unique name %s", agent.name);
        return table.get(id).obj;
    }

    @Override
    public Stream<FundsMutationAgent> streamAll() {
        return table.values()
                .getStream()
                .map(new Function<Stored<FundsMutationAgent>, FundsMutationAgent>() {
                    @Override
                    public FundsMutationAgent apply(Stored<FundsMutationAgent> stored) {
                        return FundsMutationAgent.withId(stored.obj, stored.id);
                    }
                });
    }

    @Override
    public Optional<FundsMutationAgent> findByName(String name) {
        final Integer id = nameUniqueIndex.get(name);
        return id == null
                ? Optional.<FundsMutationAgent>empty()
                : Optional.of(FundsMutationAgent.withId(table.get(id).obj, id.longValue()));
    }

    @Override
    public FundsMutationAgent getAgentWithId(FundsMutationAgent agent) {
        return findByName(agent.name).get();
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
