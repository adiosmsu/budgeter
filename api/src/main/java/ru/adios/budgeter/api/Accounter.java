package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/12/15
 * Time: 7:26 PM
 *
 * @author Mikhail Kulikov
 */
public interface Accounter {

    void registerBenefit(FundsMutationEvent mutationEvent);

    void registerLoss(FundsMutationEvent mutationEvent);

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

    void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit origin, Optional<BigDecimal> customRate);

    void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit ourCurrency, Optional<BigDecimal> customRate);

    void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate);

    Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    Stream<PostponingReasons> streamAllPostponingReasons();

    Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel);

    default Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return getStatsInTimePeriod(from, till, Optional.<FundsMutationSubject>empty());
    }

    FundsMutationSubjectRepository fundsMutationSubjectRepo();

    @Immutable
    final class PostponedMutationEvent {

        public final FundsMutationEvent mutationEvent;
        public final CurrencyUnit conversionUnit;
        public final Optional<BigDecimal> customRate;

        public PostponedMutationEvent(FundsMutationEvent mutationEvent, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
            this.customRate = customRate;
            this.mutationEvent = mutationEvent;
            this.conversionUnit = conversionUnit;
        }

    }


    @Immutable
    final class PostponedExchange {

        public final Money toBuy;
        public final CurrencyUnit unitSell;
        public final Optional<BigDecimal> customRate;

        public PostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate) {
            this.customRate = customRate;
            this.toBuy = toBuy;
            this.unitSell = unitSell;
        }

    }

    @Immutable
    final class PostponingReasons {

        public final UtcDay dayUtc;
        public final ImmutableSet<CurrencyUnit> sufferingUnits;

        public PostponingReasons(UtcDay dayUtc, ImmutableSet<CurrencyUnit> sufferingUnits) {
            this.dayUtc = dayUtc;
            this.sufferingUnits = sufferingUnits;
        }

    }

}
