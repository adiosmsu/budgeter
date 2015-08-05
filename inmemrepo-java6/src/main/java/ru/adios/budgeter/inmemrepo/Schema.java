package ru.adios.budgeter.inmemrepo;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class Schema {

    public static final FundsMutationSubjectPseudoTable FUNDS_MUTATION_SUBJECTS = FundsMutationSubjectPseudoTable.INSTANCE;
    public static final CurrencyExchangeEventPseudoTable CURRENCY_EXCHANGE_EVENTS = CurrencyExchangeEventPseudoTable.INSTANCE;
    public static final FundsMutationEventPseudoTable FUNDS_MUTATION_EVENTS = FundsMutationEventPseudoTable.INSTANCE;
    public static final PostponedCurrencyExchangeEventPseudoTable POSTPONED_CURRENCY_EXCHANGE_EVENTS = PostponedCurrencyExchangeEventPseudoTable.INSTANCE;
    public static final PostponedFundsMutationEventPseudoTable POSTPONED_FUNDS_MUTATION_EVENTS = PostponedFundsMutationEventPseudoTable.INSTANCE;
    public static final TreasuryPseudoTable TREASURY = TreasuryPseudoTable.INSTANCE;
    public static final CurrencyRatesPseudoTable CURRENCY_RATES = CurrencyRatesPseudoTable.INSTANCE;
    public static final FundsMutationAgentPseudoTable FUNDS_MUTATION_AGENTS = FundsMutationAgentPseudoTable.INSTANCE;

    public static void clearSchema() {
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
