package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableMap;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
    public Optional<FundsMutationEvent> getById(Long id) {
        final StoredFundsMutationEvent stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

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
    public Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        final int[] offsetCounter = new int[1], limitCounter = new int[1];
        offsetCounter[0] = 0; limitCounter[0] = 0;

        return table.values()
                .stream()
                .map(event -> event.obj)
                .sorted((e1, e2) -> {
                    int res = 1;
                    for (final OrderBy<Field> opt : options) {
                        switch (opt.field) {
                            case AMOUNT:
                                final int unitsComp = e1.amount.getCurrencyUnit().compareTo(e2.amount.getCurrencyUnit());
                                if (unitsComp < 0) {
                                    return -1;
                                } else if (unitsComp > 0) {
                                    res = 1;
                                    break;
                                }
                                res = opt.order.applyToCompareResult(e1.amount.compareTo(e2.amount));
                                if (res < 0) {
                                    return -1;
                                }
                                break;
                            case TIMESTAMP:
                                res = opt.order.applyToCompareResult(e1.timestamp.compareTo(e2.timestamp));
                                if (res < 0) {
                                    return -1;
                                }
                                break;
                        }
                    }
                    return res;
                })
                .filter(event1 ->
                        limit == null
                                || !(limit.offset > 0 && limit.offset > offsetCounter[0]++)
                                && !(limit.limit > 0 && limit.limit < ++limitCounter[0])
                );
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
        return table.values()
                .stream()
                .filter(event -> new UtcDay(event.obj.timestamp).equals(day))
                .map(storedFundsMutationEvent -> storedFundsMutationEvent.obj);
    }

}
