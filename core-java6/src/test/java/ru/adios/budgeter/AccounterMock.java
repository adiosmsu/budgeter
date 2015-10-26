package ru.adios.budgeter;

import java8.util.stream.Stream;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.InnerMemoryAccounter;

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
    public CurrencyExchangeEventRepository currencyExchangeEventRepository() {
        return accounter.currencyExchangeEventRepository();
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgentRepo() {
        return accounter.fundsMutationAgentRepo();
    }

    @Override
    public FundsMutationEventRepository fundsMutationEventRepository() {
        return accounter.fundsMutationEventRepository();
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return accounter.fundsMutationSubjectRepo();
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository() {
        return accounter.postponedCurrencyExchangeEventRepository();
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEventRepository() {
        return accounter.postponedFundsMutationEventRepository();
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        return accounter.streamAllPostponingReasons();
    }

    public Stream<FundsMutationEvent> streamMutationsForDay(UtcDay day) {
        return fundsMutationEventRepository.streamForDay(day);
    }

    public Stream<CurrencyExchangeEvent> streamExchangesForDay(UtcDay day) {
        return currencyExchangeEventRepository.streamForDay(day);
    }

    public void clear() {
        fundsMutationSubjectRepository.clear();
        fundsMutationEventRepository.clear();
        currencyExchangeEventRepository.clear();
        postponedFundsMutationEventRepository.clear();
        postponedCurrencyExchangeEventRepository.clear();
    }

}
