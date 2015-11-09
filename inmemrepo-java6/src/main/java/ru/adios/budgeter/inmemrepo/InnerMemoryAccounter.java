package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.PostponedExchange;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        return streamAllPostponingReasons(false);
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons(boolean compat) {
        final HashMap<UtcDay, HashSet<CurrencyUnit>> accumulator = new HashMap<UtcDay, HashSet<CurrencyUnit>>(100);
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.streamAll().forEach(new Consumer<PostponedExchange>() {
            @Override
            public void accept(PostponedExchange postponedExchange) {
                final HashSet<CurrencyUnit> units = getUnitsAcc(accumulator, new UtcDay(postponedExchange.timestamp));
                units.add(postponedExchange.toBuyAccount.getUnit());
                units.add(postponedExchange.sellAccount.getUnit());
            }
        });
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamAll().forEach(new Consumer<PostponedMutationEvent>() {
            @Override
            public void accept(PostponedMutationEvent postponedMutationEvent) {
                final HashSet<CurrencyUnit> units = getUnitsAcc(accumulator, new UtcDay(postponedMutationEvent.mutationEvent.timestamp));
                units.add(postponedMutationEvent.mutationEvent.amount.getCurrencyUnit());
                units.add(postponedMutationEvent.conversionUnit);
            }
        });
        return StreamSupport.stream(accumulator.entrySet()).map(new Function<Map.Entry<UtcDay,HashSet<CurrencyUnit>>, PostponingReasons>() {
            @Override
            public PostponingReasons apply(Map.Entry<UtcDay, HashSet<CurrencyUnit>> entry) {
                return new PostponingReasons(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
            }
        });
    }

    private HashSet<CurrencyUnit> getUnitsAcc(HashMap<UtcDay, HashSet<CurrencyUnit>> accumulator, UtcDay utcDay) {
        HashSet<CurrencyUnit> units = accumulator.get(utcDay);
        if (units == null) {
            units = new HashSet<CurrencyUnit>(10);
            accumulator.put(utcDay, units);
        }
        return units;
    }

}
