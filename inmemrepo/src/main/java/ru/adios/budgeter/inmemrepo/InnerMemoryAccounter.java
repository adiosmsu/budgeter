package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class InnerMemoryAccounter implements Accounter {

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return Schema.FUNDS_MUTATION_SUBJECTS;
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgentRepo() {
        return Schema.FUNDS_MUTATION_AGENTS;
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till, Optional<FundsMutationSubject> parentLevel) {
        return Schema.FUNDS_MUTATION_EVENTS.getStatsInTimePeriod(from, till, parentLevel);
    }

    @Override
    public void registerBenefit(FundsMutationEvent mutationEvent) {
        Schema.FUNDS_MUTATION_EVENTS.registerBenefit(mutationEvent);
    }

    @Override
    public void registerLoss(FundsMutationEvent mutationEvent) {
        Schema.FUNDS_MUTATION_EVENTS.registerLoss(mutationEvent);
    }

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        Schema.CURRENCY_EXCHANGE_EVENTS.registerCurrencyExchange(exchangeEvent);
    }

    @Override
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit payedUnit, Optional<BigDecimal> customRate) {
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(mutationEvent, payedUnit, customRate);
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit payedUnit, Optional<BigDecimal> customRate) {
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(mutationEvent, payedUnit, customRate);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedBenefits(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedLosses(day, oneOf, secondOf);
    }

    @Override
    public void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp, FundsMutationAgent agent) {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.rememberPostponedExchange(toBuy, unitSell, customRate, timestamp, agent);
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.streamRememberedExchanges(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        final HashMap<UtcDay, HashSet<CurrencyUnit>> accumulator = new HashMap<>(100);
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.streamAll().forEach(postponedExchange -> {
            final HashSet<CurrencyUnit> units = getUnitsAcc(accumulator, new UtcDay(postponedExchange.timestamp));
            units.add(postponedExchange.toBuy.getCurrencyUnit());
            units.add(postponedExchange.unitSell);
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
