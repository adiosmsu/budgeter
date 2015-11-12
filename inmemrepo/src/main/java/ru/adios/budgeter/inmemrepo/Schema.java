/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

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
    public static final SubjectPricePseudoTable SUBJECT_PRICES = SubjectPricePseudoTable.INSTANCE;

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
    public SubjectPriceRepository subjectPrices() {
        return SUBJECT_PRICES;
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
                return;
            case SUBJECT_PRICES:
                SUBJECT_PRICES.clear();
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
        SUBJECT_PRICES.clear();
    }

    private Schema() {}

}
