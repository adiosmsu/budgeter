package ru.adios.budgeter.inmemrepo;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
final class Schema {

    static final FundsMutationSubjectPseudoTable FUNDS_MUTATION_SUBJECTS = FundsMutationSubjectPseudoTable.INSTANCE;
    static final CurrencyExchangeEventPseudoTable CURRENCY_EXCHANGE_EVENTS = CurrencyExchangeEventPseudoTable.INSTANCE;
    static final FundsMutationEventPseudoTable FUNDS_MUTATION_EVENTS = FundsMutationEventPseudoTable.INSTANCE;
    static final PostponedCurrencyExchangeEventPseudoTable POSTPONED_CURRENCY_EXCHANGE_EVENTS = PostponedCurrencyExchangeEventPseudoTable.INSTANCE;
    static final PostponedFundsMutationEventPseudoTable POSTPONED_FUNDS_MUTATION_EVENTS = PostponedFundsMutationEventPseudoTable.INSTANCE;
    static final TreasuryPseudoTable TREASURY = TreasuryPseudoTable.INSTANCE;
    static final CurrencyRatesPseudoTable CURRENCY_RATES = CurrencyRatesPseudoTable.INSTANCE;

    static void clearSchema() {
        FUNDS_MUTATION_SUBJECTS.clear();
        CURRENCY_EXCHANGE_EVENTS.clear();
        FUNDS_MUTATION_EVENTS.clear();
        POSTPONED_CURRENCY_EXCHANGE_EVENTS.clear();
        POSTPONED_FUNDS_MUTATION_EVENTS.clear();
        TREASURY.clear();
        CURRENCY_RATES.clear();
    }

    private Schema() {}

}
