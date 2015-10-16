package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:53 AM
 *
 * @author Mikhail Kulikov
 */
public interface PostponedFundsMutationEventRepository {

    void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate);

    void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate);

    Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    @Immutable
    final class PostponedMutationEvent {

        public final FundsMutationEvent mutationEvent;
        public final CurrencyUnit conversionUnit;
        public final Optional<BigDecimal> customRate;

        public PostponedMutationEvent(FundsMutationEvent mutationEvent, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
            this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.empty();
            this.mutationEvent = mutationEvent;
            this.conversionUnit = conversionUnit;
        }

    }

}
