package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.PostponedFundsMutationEventPseudoTable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 10:15 AM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventRepositoryMock implements PostponedFundsMutationEventRepository {

    private final PostponedFundsMutationEventPseudoTable table = PostponedFundsMutationEventPseudoTable.INSTANCE;

    @Override
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit origin, Optional<BigDecimal> customRate) {
        table.rememberPostponedExchangeableBenefit(mutationEvent, origin, customRate);
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit ourCurrency, Optional<BigDecimal> customRate) {
        table.rememberPostponedExchangeableLoss(mutationEvent, ourCurrency, customRate);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.streamRememberedBenefits(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.streamRememberedLosses(day, oneOf, secondOf);
    }

}
