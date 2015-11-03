package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 9:53 AM
 *
 * @author Mikhail Kulikov
 */
public interface PostponedFundsMutationEventRepository extends Provider<PostponedMutationEvent, Long> {

    void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate);

    void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate);

    Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

}
