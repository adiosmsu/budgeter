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

    void registerBenefit(FundsMutationEvent mutationEvent);

    void registerLoss(FundsMutationEvent mutationEvent);

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel);

    default Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return getStatsInTimePeriod(from, till, Optional.<FundsMutationSubject>empty());
    }

}
