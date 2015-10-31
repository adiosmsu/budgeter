package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;

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

    public static final InnerMemoryAccounter ACCOUNTER = new InnerMemoryAccounter();

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEvents() {
        return CURRENCY_EXCHANGE_EVENTS;
    }

    @Nullable
    @Override
    public TransactionalSupport getTransactionalSupport() {
        return null;
    }

    @Override
    public void setTransactionalSupport(@Nullable TransactionalSupport txSupport) {
        if (txSupport != null) {
            throw new UnsupportedOperationException("This implementation uses hash maps to store data so it's unreasonable to implement any transactional support");
        }
    }

    @Override
    public void tryExecuteInTransaction(Runnable code) {
        code.run();
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
    public InnerMemoryAccounter accounter() {
        return ACCOUNTER;
    }

    @Override
    public void clearSchema() {
        clearSchemaStatic();
    }

    @Override
    public void createSchemaIfNeeded() {}

    @Override
    public void clear(Repo repo) {
        switch (repo) {
            case FUNDS_MUTATION_SUBJECTS:
                FUNDS_MUTATION_SUBJECTS.clear();
                return;
            case CURRENCY_EXCHANGE_EVENTS:
                CURRENCY_EXCHANGE_EVENTS.clear();
                return;
            case FUNDS_MUTATION_EVENTS:
                FUNDS_MUTATION_EVENTS.clear();
                return;
            case POSTPONED_CURRENCY_EXCHANGE_EVENTS:
                POSTPONED_CURRENCY_EXCHANGE_EVENTS.clear();
                return;
            case POSTPONED_FUNDS_MUTATION_EVENTS:
                POSTPONED_FUNDS_MUTATION_EVENTS.clear();
                return;
            case TREASURY:
                TREASURY.clear();
                return;
            case CURRENCY_RATES:
                CURRENCY_RATES.clear();
                return;
            case FUNDS_MUTATION_AGENTS:
                FUNDS_MUTATION_AGENTS.clear();
        }
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
