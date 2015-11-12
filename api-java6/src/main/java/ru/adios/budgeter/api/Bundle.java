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

package ru.adios.budgeter.api;

import javax.annotation.Nullable;

/**
 * Date: 10/26/15
 * Time: 2:12 PM
 *
 * @author Mikhail Kulikov
 */
public interface Bundle {

    enum Repo {
        FUNDS_MUTATION_SUBJECTS,
        CURRENCY_EXCHANGE_EVENTS,
        FUNDS_MUTATION_EVENTS,
        POSTPONED_CURRENCY_EXCHANGE_EVENTS,
        POSTPONED_FUNDS_MUTATION_EVENTS,
        TREASURY,
        CURRENCY_RATES,
        FUNDS_MUTATION_AGENTS,
        SUBJECT_PRICES
    }

    final class Default {

        private final Bundle bundle;

        public Default(Bundle bundle) {
            this.bundle = bundle;
        }

        public void tryExecuteInTransaction(Runnable code) {
            final TransactionalSupport txSupport = bundle.getTransactionalSupport();
            if (txSupport != null) {
                txSupport.runWithTransaction(code);
            } else {
                code.run();
            }
        }

    }

    @Nullable
    TransactionalSupport getTransactionalSupport();

    void setTransactionalSupport(@Nullable TransactionalSupport txSupport);

    void tryExecuteInTransaction(Runnable code); // default in java8

    FundsMutationSubjectRepository fundsMutationSubjects();

    CurrencyExchangeEventRepository currencyExchangeEvents();

    FundsMutationEventRepository fundsMutationEvents();

    PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents();

    PostponedFundsMutationEventRepository postponedFundsMutationEvents();

    Treasury treasury();

    CurrencyRatesRepository currencyRates();

    FundsMutationAgentRepository fundsMutationAgents();

    Accounter accounter();

    SubjectPriceRepository subjectPrices();

    void clearSchema();

    void createSchemaIfNeeded();

    void clear(Repo repo);

}
