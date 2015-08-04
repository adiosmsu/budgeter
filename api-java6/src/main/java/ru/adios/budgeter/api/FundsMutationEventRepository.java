package ru.adios.budgeter.api;

import org.joda.money.Money;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Date: 6/15/15
 * Time: 9:50 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationEventRepository {

    final class Default {

        private final FundsMutationEventRepository fundsMutationEventRepository;

        public Default(FundsMutationEventRepository fundsMutationEventRepository) {
            this.fundsMutationEventRepository = fundsMutationEventRepository;
        }

        public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
            return fundsMutationEventRepository.getStatsInTimePeriod(from, till, Optional.<FundsMutationSubject>empty());
        }

    }

    void registerBenefit(FundsMutationEvent mutationEvent);

    void registerLoss(FundsMutationEvent mutationEvent);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till); // default in java8

}
