package ru.adios.budgeter.api.data;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Date: 11/3/15
 * Time: 10:57 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class PostponedMutationEvent {

    public final FundsMutationEvent mutationEvent;
    public final CurrencyUnit conversionUnit;
    public final Optional<BigDecimal> customRate;

    public PostponedMutationEvent(FundsMutationEvent mutationEvent, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.empty();
        this.mutationEvent = mutationEvent;
        this.conversionUnit = conversionUnit;
    }

}
