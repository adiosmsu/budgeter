package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.*;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class Schema implements Bundle {

    public static final Schema INSTANCE = new Schema();

    public static final FundsMutationSubjectPseudoTable FUNDS_MUTATION_SUBJECTS = FundsMutationSubjectPseudoTable.INSTANCE;
    public static final CurrencyExchangeEventPseudoTable CURRENCY_EXCHANGE_EVENTS = CurrencyExchangeEventPseudoTable.INSTANCE;
    public static final FundsMutationEventPseudoTable FUNDS_MUTATION_EVENTS = FundsMutationEventPseudoTable.INSTANCE;
    public static final PostponedCurrencyExchangeEventPseudoTable POSTPONED_CURRENCY_EXCHANGE_EVENTS = PostponedCurrencyExchangeEventPseudoTable.INSTANCE;
    public static final PostponedFundsMutationEventPseudoTable POSTPONED_FUNDS_MUTATION_EVENTS = PostponedFundsMutationEventPseudoTable.INSTANCE;
    public static final TreasuryPseudoTable TREASURY = TreasuryPseudoTable.INSTANCE;
    public static final CurrencyRatesPseudoTable CURRENCY_RATES = CurrencyRatesPseudoTable.INSTANCE;
    public static final FundsMutationAgentPseudoTable FUNDS_MUTATION_AGENTS = FundsMutationAgentPseudoTable.INSTANCE;

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEvents() {
        return CURRENCY_EXCHANGE_EVENTS;
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjects() {
        return FUNDS_MUTATION_SUBJECTS;
    }

    @Override
    public FundsMutationEventRepository fundsMutationEvents() {
        return FUNDS_MUTATION_EVENTS;
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents() {
        return POSTPONED_CURRENCY_EXCHANGE_EVENTS;
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEvents() {
        return POSTPONED_FUNDS_MUTATION_EVENTS;
    }

    @Override
    public Treasury treasury() {
        return TREASURY;
    }

    @Override
    public CurrencyRatesRepository currencyRates() {
        return CURRENCY_RATES;
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgents() {
        return FUNDS_MUTATION_AGENTS;
    }

    @Override
    public void clearSchema() {
        clearSchemaStatic();
    }

    public static void clearSchemaStatic() {
        FUNDS_MUTATION_SUBJECTS.clear();
        CURRENCY_EXCHANGE_EVENTS.clear();
        FUNDS_MUTATION_EVENTS.clear();
        POSTPONED_CURRENCY_EXCHANGE_EVENTS.clear();
        POSTPONED_FUNDS_MUTATION_EVENTS.clear();
        TREASURY.clear();
        CURRENCY_RATES.clear();
        FUNDS_MUTATION_AGENTS.clear();
    }

    private Schema() {}

}
