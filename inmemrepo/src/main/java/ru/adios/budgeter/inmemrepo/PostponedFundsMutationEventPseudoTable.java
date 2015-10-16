package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:16 AM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedFundsMutationEventPseudoTable
        extends AbstractPseudoTable<StoredPostponedFundsMutationEvent, PostponedFundsMutationEventRepository.PostponedMutationEvent>
        implements PostponedFundsMutationEventRepository
{

    public static final PostponedFundsMutationEventPseudoTable INSTANCE = new PostponedFundsMutationEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, StoredPostponedFundsMutationEvent> table = new ConcurrentHashMap<>(100, 0.75f, 4);

    private PostponedFundsMutationEventPseudoTable() {}

    @Override
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        store(storedFactory(mutationEvent, FundsMutationDirection.BENEFIT, paidUnit, customRate));
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        store(storedFactory(mutationEvent, FundsMutationDirection.LOSS, paidUnit, customRate));
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamRemembered(day, oneOf, secondOf, FundsMutationDirection.BENEFIT);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamRemembered(day, oneOf, secondOf, FundsMutationDirection.LOSS);
    }

    Stream<PostponedMutationEvent> streamAll() {
        return table.values().stream().map(storedPostponedFundsMutationEvent -> storedPostponedFundsMutationEvent.obj);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, StoredPostponedFundsMutationEvent> innerTable() {
        return table;
    }

    @Override
    public void clear() {
        table.clear();
    }

    private Stream<PostponedMutationEvent> streamRemembered(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf, FundsMutationDirection direction) {
        return table.values().stream().filter(event -> {
            final CurrencyUnit amountUnit = event.obj.mutationEvent.amount.getCurrencyUnit();
            return event.direction == direction
                    && day.equals(new UtcDay(event.obj.mutationEvent.timestamp))
                    && (amountUnit.equals(oneOf) || amountUnit.equals(secondOf))
                    && (event.obj.conversionUnit.equals(oneOf) || event.obj.conversionUnit.equals(secondOf));
        }).map(event -> event.obj);
    }

    private void store(StoredPostponedFundsMutationEvent event) {
        checkState(table.putIfAbsent(event.id, event) == null);
    }

    private StoredPostponedFundsMutationEvent storedFactory(FundsMutationEvent event, FundsMutationDirection direction, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
        return new StoredPostponedFundsMutationEvent(idSequence.incrementAndGet(), event, direction, conversionUnit, customRate);
    }

}
