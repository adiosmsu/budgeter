package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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

    final class Default {

        private final FundsMutationEventRepository fundsMutationEventRepository;

        public Default(FundsMutationEventRepository fundsMutationEventRepository) {
            this.fundsMutationEventRepository = fundsMutationEventRepository;
        }

        public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
            return fundsMutationEventRepository.getStatsInTimePeriod(from, till, Optional.<FundsMutationSubject>empty());
        }

        public Stream<FundsMutationEvent> streamMutationEvents(RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return fundsMutationEventRepository.streamMutationEvents(pair.options, pair.limit);
        }

    }

    void register(FundsMutationEvent mutationEvent);

    Stream<FundsMutationEvent> streamMutationEvents(RepoOption... options); // default in java8

    Stream<FundsMutationEvent> streamMutationEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit);

    Stream<FundsMutationEvent> streamForDay(UtcDay day);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till); // default in java8

}
