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

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class InnerMemoryAccounter implements Accounter {

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEventRepository() {
        return Schema.CURRENCY_EXCHANGE_EVENTS;
    }

    @Override
    public FundsMutationEventRepository fundsMutationEventRepository() {
        return Schema.FUNDS_MUTATION_EVENTS;
    }

    @Override
    public SubjectPriceRepository subjectPriceRepository() {
        return Schema.SUBJECT_PRICES;
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEventRepository() {
        return Schema.POSTPONED_FUNDS_MUTATION_EVENTS;
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository() {
        return Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS;
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return Schema.FUNDS_MUTATION_SUBJECTS;
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgentRepo() {
        return Schema.FUNDS_MUTATION_AGENTS;
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons(boolean compatMode) {
        final HashMap<UtcDay, HashSet<CurrencyUnit>> accumulator = new HashMap<>(100);
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.streamAll().forEach(postponedExchange -> {
            final HashSet<CurrencyUnit> units = getUnitsAcc(accumulator, new UtcDay(postponedExchange.timestamp));
            units.add(postponedExchange.toBuyAccount.getUnit());
            units.add(postponedExchange.sellAccount.getUnit());
        });
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamAll().forEach(postponedMutationEvent -> {
            final HashSet<CurrencyUnit> units = getUnitsAcc(accumulator, new UtcDay(postponedMutationEvent.mutationEvent.timestamp));
            units.add(postponedMutationEvent.mutationEvent.amount.getCurrencyUnit());
            units.add(postponedMutationEvent.conversionUnit);
        });
        return accumulator
                .entrySet()
                .stream()
                .map(entry -> new PostponingReasons(entry.getKey(), ImmutableSet.copyOf(entry.getValue())));
    }

    private HashSet<CurrencyUnit> getUnitsAcc(HashMap<UtcDay, HashSet<CurrencyUnit>> accumulator, UtcDay utcDay) {
        HashSet<CurrencyUnit> units = accumulator.get(utcDay);
        if (units == null) {
            units = new HashSet<>(10);
            accumulator.put(utcDay, units);
        }
        return units;
    }

}
