package ru.adios.budgeter.api;

/**
 * Date: 10/26/15
 * Time: 2:12 PM
 *
 * @author Mikhail Kulikov
 */
public interface Bundle {

    FundsMutationSubjectRepository fundsMutationSubjects();

    CurrencyExchangeEventRepository currencyExchangeEvents();

    FundsMutationEventRepository fundsMutationEvents();

    PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents();

    PostponedFundsMutationEventRepository postponedFundsMutationEvents();

    Treasury treasury();

    CurrencyRatesRepository currencyRates();

    FundsMutationAgentRepository fundsMutationAgents();

    void clearSchema();

}
