package ru.adios.budgeter;

import org.joda.money.Money;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.FundsMutationEventPseudoTable;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return table.getStatsInTimePeriod(from, till);
    }

    public Stream<FundsMutationEvent> streamForDay(UtcDay day) {
        return table.streamForDay(day);
    }

    @Override
    public Stream<FundsMutationEvent> stream(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return table.stream(options, limit);
    }

    public void clear() {
        table.clear();
    }

}
