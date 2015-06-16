package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.InnerMemoryAccounter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:00 AM
 *
 * @author Mikhail Kulikov
 */
public class AccounterMock implements Accounter {

    private final FundsMutationSubjectRepositoryMock fundsMutationSubjectRepository = new FundsMutationSubjectRepositoryMock();
    private final FundsMutationEventRepositoryMock fundsMutationEventRepository = new FundsMutationEventRepositoryMock();
    private final CurrencyExchangeEventRepositoryMock currencyExchangeEventRepository = new CurrencyExchangeEventRepositoryMock();
    private final PostponedFundsMutationEventRepositoryMock postponedFundsMutationEventRepository = new PostponedFundsMutationEventRepositoryMock();
    private final PostponedCurrencyExchangeEventRepositoryMock postponedCurrencyExchangeEventRepository = new PostponedCurrencyExchangeEventRepositoryMock();
    private final InnerMemoryAccounter accounter = new InnerMemoryAccounter();

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return fundsMutationSubjectRepository;
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return fundsMutationEventRepository.getStatsInTimePeriod(from, till, parentLevel);
    }

    @Override
    public void registerBenefit(FundsMutationEvent mutationEvent) {
        fundsMutationEventRepository.registerBenefit(mutationEvent);
    }

    @Override
    public void registerLoss(FundsMutationEvent mutationEvent) {
        fundsMutationEventRepository.registerLoss(mutationEvent);
    }

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        currencyExchangeEventRepository.registerCurrencyExchange(exchangeEvent);
    }

    @Override
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit origin, Optional<BigDecimal> customRate) {
        postponedFundsMutationEventRepository.rememberPostponedExchangeableBenefit(mutationEvent, origin, customRate);
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit ourCurrency, Optional<BigDecimal> customRate) {
        postponedFundsMutationEventRepository.rememberPostponedExchangeableLoss(mutationEvent, ourCurrency, customRate);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return postponedFundsMutationEventRepository.streamRememberedBenefits(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return postponedFundsMutationEventRepository.streamRememberedLosses(day, oneOf, secondOf);
    }

    @Override
    public void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp) {
        postponedCurrencyExchangeEventRepository.rememberPostponedExchange(toBuy, unitSell, customRate, timestamp);
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return postponedCurrencyExchangeEventRepository.streamRememberedExchanges(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        return accounter.streamAllPostponingReasons();
    }

}
