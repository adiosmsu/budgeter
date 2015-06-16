package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableMap;
import org.joda.money.Money;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.FundsMutationSubject;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentHashMap<Integer, StoredFundsMutationEvent> table = new ConcurrentHashMap<>(100, 0.75f, 4);

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

    private void store(StoredFundsMutationEvent event) {
        Schema.FUNDS_MUTATION_SUBJECTS.findByName(event.obj.subject.name).orElseThrow(() -> new IllegalStateException("No subject with name " + event.obj.subject.name));
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

}
