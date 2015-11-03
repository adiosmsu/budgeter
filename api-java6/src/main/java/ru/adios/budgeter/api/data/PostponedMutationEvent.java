package ru.adios.budgeter.api.data;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 11/3/15
 * Time: 11:01 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class PostponedMutationEvent {

    public final FundsMutationEvent mutationEvent;
    public final CurrencyUnit conversionUnit;
    public final Optional<BigDecimal> customRate;

    public PostponedMutationEvent(FundsMutationEvent mutationEvent, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.<BigDecimal>empty();
        this.mutationEvent = mutationEvent;
        this.conversionUnit = conversionUnit;
    }

}
