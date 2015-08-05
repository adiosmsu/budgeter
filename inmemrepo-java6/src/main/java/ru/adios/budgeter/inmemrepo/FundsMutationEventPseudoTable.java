package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 9:59 AM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationEventPseudoTable extends AbstractPseudoTable<StoredFundsMutationEvent, FundsMutationEvent> implements FundsMutationEventRepository {

    public static final FundsMutationEventPseudoTable INSTANCE = new FundsMutationEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, StoredFundsMutationEvent> table = new ConcurrentHashMap<Integer, StoredFundsMutationEvent>(100, 0.75f, 4);
    private final FundsMutationEventRepository.Default fmeRepoDef = new Default(this);

    private FundsMutationEventPseudoTable() {}

    @Override
    public void registerBenefit(FundsMutationEvent mutationEvent) {
        store(storedFactory(mutationEvent, FundsMutationDirection.BENEFIT));
    }

    @Override
    public void registerLoss(FundsMutationEvent mutationEvent) {
        store(storedFactory(mutationEvent, FundsMutationDirection.LOSS));
    }

    private StoredFundsMutationEvent storedFactory(FundsMutationEvent mutationEvent, FundsMutationDirection direction) {
        return new StoredFundsMutationEvent(idSequence.incrementAndGet(), mutationEvent, direction);
    }

    private void store(final StoredFundsMutationEvent event) {
        Schema.FUNDS_MUTATION_SUBJECTS.findByName(event.obj.subject.name).orElseThrow(new Supplier<IllegalStateException>() {
            @Override
            public IllegalStateException get() {
                return new IllegalStateException("No subject with name " + event.obj.subject.name);
            }
        });
        checkState(table.putIfAbsent(event.id, event) == null);
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return ImmutableMap.of();
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, StoredFundsMutationEvent> innerTable() {
        return table;
    }

    public Stream<FundsMutationEvent> streamForDay(final UtcDay day) {
        return StreamSupport.stream(table.values().getSpliterator(), false)
                .filter(new Predicate<StoredFundsMutationEvent>() {
                    @Override
                    public boolean test(StoredFundsMutationEvent event) {
                        return new UtcDay(event.obj.timestamp).equals(day);
                    }
                })
                .map(new Function<StoredFundsMutationEvent, FundsMutationEvent>() {
                    @Override
                    public FundsMutationEvent apply(StoredFundsMutationEvent event) {
                        return event.obj;
                    }
                });
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return fmeRepoDef.getStatsInTimePeriod(from, till);
    }

}
