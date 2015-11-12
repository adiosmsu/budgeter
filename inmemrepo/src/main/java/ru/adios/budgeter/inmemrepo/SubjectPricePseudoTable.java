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

import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.SubjectPriceRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 11/9/15
 * Time: 11:36 AM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPricePseudoTable extends AbstractPseudoTable<Stored<SubjectPrice>, SubjectPrice> implements SubjectPriceRepository {

    public static final SubjectPricePseudoTable INSTANCE = new SubjectPricePseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<SubjectPrice>> table = new ConcurrentHashMap<>(100, 0.75f, 4);
    private final ConcurrentHashMap<CompositeKey, Integer> uniqueConstraint = new ConcurrentHashMap<>(100, 0.75f, 4);

    private SubjectPricePseudoTable() {}

    @Override
    public void register(SubjectPrice subjectPrice) {
        checkArgument(subjectPrice != null, "subjectPrice is null");
        final int id = idSequence.incrementAndGet();

        final CompositeKey key = new CompositeKey(subjectPrice);
        checkState(uniqueConstraint.computeIfAbsent(key, compositeKey -> {
            Stored<SubjectPrice> stored = new Stored<>(id, subjectPrice);
            Schema.FUNDS_MUTATION_SUBJECTS.findByName(stored.obj.subject.name)
                    .orElseThrow(() -> new IllegalStateException("No subject with name " + stored.obj.subject.name));
            Schema.FUNDS_MUTATION_AGENTS.findByName(stored.obj.agent.name)
                    .orElseThrow(() -> new IllegalStateException("No agent with name " + stored.obj.agent.name));

            checkState(table.putIfAbsent(id, stored) == null, "Id %s already present", id);
            return id;
        }).equals(id), "Non unique insert of %s on place of id %s", subjectPrice, table.get(uniqueConstraint.get(key)));
    }

    @Override
    public boolean priceExists(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day) {
        return uniqueConstraint.containsKey(new CompositeKey(subject, agent, day));
    }

    @Override
    public int countByAgent(long subjectId, long agentId) {
        return countByAgent(Schema.FUNDS_MUTATION_SUBJECTS.getById(subjectId).get(), Schema.FUNDS_MUTATION_AGENTS.getById(agentId).get());
    }

    @Override
    public int countByAgent(FundsMutationSubject subject, FundsMutationAgent agent) {
        return (int) table.values()
                .stream()
                .filter(stored -> stored.obj.subject.equals(subject) && stored.obj.agent.equals(agent))
                .count();
    }

    @Override
    public int countByAgent(String subjectName, String agentName) {
        return countByAgent(Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get(), Schema.FUNDS_MUTATION_AGENTS.findByName(agentName).get());
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return table.values()
                .stream()
                .filter(stored -> stored.obj.subject.equals(subject) && stored.obj.agent.equals(agent))
                .map(subjectPriceStored -> subjectPriceStored.obj)
                .sorted(new OrderByComparator(options))
                .filter(new LimitingPredicate<>(limit));
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return streamByAgent(Schema.FUNDS_MUTATION_SUBJECTS.getById(subjectId).get(), Schema.FUNDS_MUTATION_AGENTS.getById(agentId).get(), options, limit);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return streamByAgent(
                Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get(), Schema.FUNDS_MUTATION_AGENTS.findByName(agentName).get(), options, limit
        );
    }

    @Override
    public int count(long subjectId) {
        return count(Schema.FUNDS_MUTATION_SUBJECTS.getById(subjectId).get());
    }

    @Override
    public int count(FundsMutationSubject subject) {
        return (int) table.values()
                .stream()
                .filter(stored -> stored.obj.subject.equals(subject))
                .count();
    }

    @Override
    public int count(String subjectName) {
        return count(Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get());
    }

    @Override
    public Stream<SubjectPrice> stream(long subjectId, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return stream(Schema.FUNDS_MUTATION_SUBJECTS.getById(subjectId).get(), options, limit);
    }

    @Override
    public Stream<SubjectPrice> stream(FundsMutationSubject subject, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return table.values()
                .stream()
                .filter(stored -> stored.obj.subject.equals(subject))
                .map(subjectPriceStored -> subjectPriceStored.obj)
                .sorted(new OrderByComparator(options))
                .filter(new LimitingPredicate<>(limit));
    }

    @Override
    public Stream<SubjectPrice> stream(String subjectName, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return stream(Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get(), options, limit);
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public Optional<SubjectPrice> getById(Long id) {
        final Stored<SubjectPrice> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<SubjectPrice>> innerTable() {
        return table;
    }

    @Override
    public void clear() {
        table.clear();
        uniqueConstraint.clear();
    }

    private static final class OrderByComparator implements Comparator<SubjectPrice> {

        private final List<OrderBy<Field>> options;

        private OrderByComparator(List<OrderBy<Field>> options) {
            this.options = options;
        }

        @Override
        public int compare(SubjectPrice o1, SubjectPrice o2) {
            int res = 1;
            for (final OrderBy<Field> opt : options) {
                switch (opt.field) {
                    case PRICE:
                        final int unitsComp = o1.price.getCurrencyUnit().compareTo(o2.price.getCurrencyUnit());
                        if (unitsComp < 0) {
                            return -1;
                        } else if (unitsComp > 0) {
                            res = 1;
                            break;
                        }
                        res = opt.order.applyToCompareResult(o1.price.compareTo(o2.price));
                        if (res < 0) {
                            return -1;
                        }
                        break;
                    case DAY:
                        res = opt.order.applyToCompareResult(o1.day.compareTo(o2.day));
                        if (res < 0) {
                            return -1;
                        }
                        break;
                }
            }
            return res;
        }

    }

    private static final class CompositeKey {

        private final int code;

        private CompositeKey(SubjectPrice subjectPrice) {
            this(subjectPrice.subject, subjectPrice.agent, subjectPrice.day);
        }

        private CompositeKey(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day) {
            int result = day.hashCode();
            result = 31 * result + subject.hashCode();
            result = 31 * result + agent.hashCode();
            code = result;
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || !(o == null || getClass() != o.getClass())
                    && code == ((CompositeKey) o).code;
        }

        @Override
        public int hashCode() {
            return code;
        }

    }

}
