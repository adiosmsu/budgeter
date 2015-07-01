package ru.adios.budgeter;

import org.joda.money.Money;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.FundsMutationEventPseudoTable;

import java.time.OffsetDateTime;
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

    public void clear() {
        table.clear();
    }

}
