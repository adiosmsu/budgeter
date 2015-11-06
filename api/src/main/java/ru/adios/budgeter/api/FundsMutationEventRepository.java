package ru.adios.budgeter.api;

import org.joda.money.Money;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:50 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationEventRepository extends Provider<FundsMutationEvent, Long> {

    enum Field implements OrderedField {
        TIMESTAMP, AMOUNT
    }

    void register(FundsMutationEvent mutationEvent);

    default Stream<FundsMutationEvent> streamMutationEvents(RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamMutationEvents(pair.options, pair.limit);
    }

    int countMutationEvents();

    Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<FundsMutationEvent> streamForDay(UtcDay day);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel);

    default Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return getStatsInTimePeriod(from, till, Optional.<FundsMutationSubject>empty());
    }

}
