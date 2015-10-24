package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.FundsMutationEventPseudoTable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Date: 6/15/15
 * Time: 9:59 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventRepositoryMock implements FundsMutationEventRepository {

    private final FundsMutationEventPseudoTable table = FundsMutationEventPseudoTable.INSTANCE;

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return table.getStatsInTimePeriod(from, till, parentLevel);
    }

    @Override
    public void registerBenefit(FundsMutationEvent mutationEvent) {
        table.registerBenefit(mutationEvent);
    }

    @Override
    public void registerLoss(FundsMutationEvent mutationEvent) {
        table.registerLoss(mutationEvent);
    }

    @Override
    public Stream<FundsMutationEvent> streamMutationEvents(RepoOption... options) {
        return table.streamMutationEvents(options);
    }

    @Override
    public Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return table.streamMutationEvents(options, limit);
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return table.getStatsInTimePeriod(from, till);
    }

    public Stream<FundsMutationEvent> streamForDay(UtcDay day) {
        return table.streamForDay(day);
    }

    public void clear() {
        table.clear();
    }

}
