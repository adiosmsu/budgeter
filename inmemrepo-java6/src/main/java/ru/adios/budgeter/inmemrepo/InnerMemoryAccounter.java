package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Date: 6/15/15
 * Time: 12:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class InnerMemoryAccounter implements Accounter {

    private final FundsMutationEventRepository.Default fmeRepoDef = new Default(this);

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
    public Stream<FundsMutationEvent> stream(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return Schema.FUNDS_MUTATION_EVENTS.stream(options, limit);
    }

    @Override
    public Stream<FundsMutationEvent> stream(RepoOption... options) {
        return Schema.FUNDS_MUTATION_EVENTS.stream(options);
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
    public void rememberPostponedExchangeableBenefit(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(mutationEvent, paidUnit, customRate);
    }

    @Override
    public void rememberPostponedExchangeableLoss(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(mutationEvent, paidUnit, customRate);
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
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          Treasury.BalanceAccount toBuyAccount,
                                          Treasury.BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent)
    {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.rememberPostponedExchange(toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent);
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.streamRememberedExchanges(day, oneOf, secondOf);
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
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
        return StreamSupport.stream(accumulator.entrySet()).map(new Function<Map.Entry<UtcDay, HashSet<CurrencyUnit>>, PostponingReasons>() {
            @Override
            public PostponingReasons apply(Map.Entry<UtcDay, HashSet<CurrencyUnit>> entry) {
                return new PostponingReasons(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
            }
        });
    }

    @Override
    public Map<FundsMutationSubject, Money> getStatsInTimePeriod(OffsetDateTime from, OffsetDateTime till) {
        return fmeRepoDef.getStatsInTimePeriod(from, till);
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
