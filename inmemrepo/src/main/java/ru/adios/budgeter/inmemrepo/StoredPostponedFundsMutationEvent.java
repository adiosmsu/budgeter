package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Date: 6/15/15
 * Time: 10:16 AM
 *
 * @author Mikhail Kulikov
 */
final class StoredPostponedFundsMutationEvent extends Stored<PostponedMutationEvent> {

    final FundsMutationDirection direction;

    StoredPostponedFundsMutationEvent(int id, FundsMutationEvent obj, FundsMutationDirection direction, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
        super(id, new PostponedMutationEvent(obj, conversionUnit, customRate));
        this.direction = direction;
    }

}

