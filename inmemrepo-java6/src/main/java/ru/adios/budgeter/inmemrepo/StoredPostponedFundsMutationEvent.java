package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 10:16 AM
 *
 * @author Mikhail Kulikov
 */
final class StoredPostponedFundsMutationEvent extends Stored<PostponedFundsMutationEventRepository.PostponedMutationEvent> {

    final FundsMutationDirection direction;

    StoredPostponedFundsMutationEvent(int id, FundsMutationEvent obj, FundsMutationDirection direction, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate) {
        super(id, new PostponedFundsMutationEventRepository.PostponedMutationEvent(obj, conversionUnit, customRate));
        this.direction = direction;
    }

}

