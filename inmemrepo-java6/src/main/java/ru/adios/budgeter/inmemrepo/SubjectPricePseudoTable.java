package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ConcurrentHashMap<Integer, Stored<SubjectPrice>> table = new ConcurrentHashMap<Integer, Stored<SubjectPrice>>(100, 0.75f, 4);
    private final ConcurrentHashMap<CompositeKey, Integer> uniqueConstraint = new ConcurrentHashMap<CompositeKey, Integer>(100, 0.75f, 4);
    private final Default def = new Default(this);

    private SubjectPricePseudoTable() {}

    @Override
    public void register(final SubjectPrice subjectPrice) {
        checkArgument(subjectPrice != null, "subjectPrice is null");
        final int id = idSequence.incrementAndGet();

        final CompositeKey key = new CompositeKey(subjectPrice);
        checkState(uniqueConstraint.computeIfAbsent(key, new Function<CompositeKey, Integer>() {
            @Override
            public Integer apply(CompositeKey compositeKey) {
                final Stored<SubjectPrice> stored = new Stored<SubjectPrice>(id, subjectPrice);
                Schema.FUNDS_MUTATION_SUBJECTS.findByName(stored.obj.subject.name)
                        .orElseThrow(new Supplier<IllegalStateException>() {
                            @Override
                            public IllegalStateException get() {
                                return new IllegalStateException("No subject with name " + stored.obj.subject.name);
                            }
                        });
                Schema.FUNDS_MUTATION_AGENTS.findByName(stored.obj.agent.name)
                        .orElseThrow(new Supplier<IllegalStateException>() {
                            @Override
                            public IllegalStateException get() {
                                return new IllegalStateException("No agent with name " + stored.obj.agent.name);
                            }
                        });

                checkState(table.putIfAbsent(id, stored) == null, "Id %s already present", id);
                return id;
            }
        }).equals(id), "Non unique insert of %s on place of id %s", subjectPrice, table.get(uniqueConstraint.get(key)));
    }

    @Override
    public boolean priceExists(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day) {
        return uniqueConstraint.containsKey(new CompositeKey(subject, agent, day));
    }

    @Override
    public int countByAgent(final FundsMutationSubject subject, final FundsMutationAgent agent) {
        return (int) table.values()
                .getStream()
                .filter(new Predicate<Stored<SubjectPrice>>() {
                    @Override
                    public boolean test(Stored<SubjectPrice> stored) {
                        return stored.obj.subject.equals(subject) && stored.obj.agent.equals(agent);
                    }
                })
                .count();
    }

    @Override
    public int countByAgent(String subjectName, String agentName) {
        return countByAgent(Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get(), Schema.FUNDS_MUTATION_AGENTS.findByName(agentName).get());
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(final FundsMutationSubject subject, final FundsMutationAgent agent, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return table.values()
                .getStream()
                .filter(new Predicate<Stored<SubjectPrice>>() {
                    @Override
                    public boolean test(Stored<SubjectPrice> stored) {
                        return stored.obj.subject.equals(subject) && stored.obj.agent.equals(agent);
                    }
                })
                .map(new Function<Stored<SubjectPrice>, SubjectPrice>() {
                    @Override
                    public SubjectPrice apply(Stored<SubjectPrice> stored) {
                        return stored.obj;
                    }
                })
                .sorted(new OrderByComparator(options))
                .filter(new LimitingPredicate<SubjectPrice>(limit));
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return streamByAgent(
                Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get(), Schema.FUNDS_MUTATION_AGENTS.findByName(agentName).get(), options, limit
        );
    }

    @Override
    public int count(final FundsMutationSubject subject) {
        return (int) table.values()
                .getStream()
                .filter(new Predicate<Stored<SubjectPrice>>() {
                    @Override
                    public boolean test(Stored<SubjectPrice> stored) {
                        return stored.obj.subject.equals(subject);
                    }
                })
                .count();
    }

    @Override
    public int count(String subjectName) {
        return count(Schema.FUNDS_MUTATION_SUBJECTS.findByName(subjectName).get());
    }

    @Override
    public Stream<SubjectPrice> stream(final FundsMutationSubject subject, List<OrderBy<Field>> options, Optional<OptLimit> limit) {
        return table.values()
                .getStream()
                .filter(new Predicate<Stored<SubjectPrice>>() {
                    @Override
                    public boolean test(Stored<SubjectPrice> stored) {
                        return stored.obj.subject.equals(subject);
                    }
                })
                .map(new Function<Stored<SubjectPrice>, SubjectPrice>() {
                    @Override
                    public SubjectPrice apply(Stored<SubjectPrice> stored) {
                        return stored.obj;
                    }
                })
                .sorted(new OrderByComparator(options))
                .filter(new LimitingPredicate<SubjectPrice>(limit));
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

    @Override
    public Stream<SubjectPrice> stream(FundsMutationSubject subject, RepoOption... options) {
        return def.stream(subject, options);
    }

    @Override
    public Stream<SubjectPrice> stream(String subjectName, RepoOption... options) {
        return def.stream(subjectName, options);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, RepoOption... options) {
        return def.streamByAgent(subject, agent, options);
    }

    @Override
    public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, RepoOption... options) {
        return def.streamByAgent(subjectName, agentName, options);
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
