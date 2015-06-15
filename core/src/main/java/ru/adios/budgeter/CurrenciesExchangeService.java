package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.adios.budgeter.api.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Date: 6/14/15
 * Time: 6:01 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrenciesExchangeService implements CurrencyRatesProvider {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int counter = 0;

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            final Thread thread = new Thread(r, "currenciesExecutorThread0" + ++counter);
            thread.setDaemon(true);
            return thread;
        }
    });

    @Autowired private volatile CurrencyRatesRepository ratesRepository;
    @Autowired private volatile Accounter accounter;
    @Autowired private volatile Treasury treasury;
    @Autowired private volatile ExchangeRatesLoader.BtcLoader btcLoader;
    @Autowired private volatile ExchangeRatesLoader.CbrLoader cbrLoader;

    @Transactional
    public void runWithTransaction(ImmutableList<Runnable> runnables) {
        runnables.forEach(java.lang.Runnable::run);
    }

    public final void processAllPostponedEvents() {
        accounter.streamAllPostponingReasons().forEach(
                postponingReasons -> CurrencyRatesProvider.streamConversionPairs(postponingReasons.sufferingUnits)
                        .forEach(conversionPair -> getConversionMultiplier(postponingReasons.dayUtc, conversionPair.from, conversionPair.to, false))
        );
    }

    @Override
    public final Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(day, from, to, true);
    }

    private Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to, boolean attemptToLoadFromRepo) {
        final ImmutableList.Builder<Runnable> tasksBuilder = ImmutableList.builder();

        try {
            final CurrencyUnit btcUnit = btcLoader.getMainUnit();
            if (from.equals(btcUnit) || to.equals(btcUnit)) {
                // btc case
                final CurrencyUnit other = from.equals(btcUnit)
                        ? to
                        : from;

                if (day.equals(new UtcDay())) {
                    // only momentary rates for btc, return right away
                    return loadResult(btcLoader, day, other, btcUnit, from, to, tasksBuilder);
                }

                return conversionMultiplierFor(btcLoader, day, other, btcUnit, from, to, tasksBuilder, attemptToLoadFromRepo);
            }

            final CurrencyUnit rubUnit = cbrLoader.getMainUnit();
            if (from.equals(rubUnit) || to.equals(rubUnit)) {
                // rub case
                final CurrencyUnit other = from.equals(rubUnit)
                        ? to
                        : from;

                return conversionMultiplierFor(cbrLoader, day, other, rubUnit, from, to, tasksBuilder, attemptToLoadFromRepo);
            }

            // arbitrary case
            final Optional<BigDecimal> rubToFrom = conversionMultiplierFor(cbrLoader, day, from, rubUnit, rubUnit, from, tasksBuilder, attemptToLoadFromRepo);
            final Optional<BigDecimal> rubToTo = conversionMultiplierFor(cbrLoader, day, to, rubUnit, rubUnit, to, tasksBuilder, attemptToLoadFromRepo);
            if (rubToFrom.isPresent() && rubToTo.isPresent()) {
                return Optional.of(CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(rubToFrom.get(), rubToTo.get()));
            }

            return Optional.empty(); // nothing worked :(
        } finally {
            final ImmutableList<Runnable> tasks = tasksBuilder.build();
            if (tasks.size() > 0)
                executor.submit(() -> runWithTransaction(tasks));
        }
    }

    @Override
    public final BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return ratesRepository.getLatestConversionMultiplier(from, to);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return ratesRepository.getLatestOptionalConversionMultiplier(from, to);
    }

    @Override
    public boolean isRateStale(CurrencyUnit to) {
        return false;
    }

    private Optional<BigDecimal> conversionMultiplierFor(
            ExchangeRatesLoader loader, UtcDay day, CurrencyUnit other, CurrencyUnit mainUnit, CurrencyUnit from, CurrencyUnit to, ImmutableList.Builder<Runnable> tasksBuilder, boolean attempt
    )
    {
        if (attempt) {
            final Optional<BigDecimal> resultRef = ratesRepository.getConversionMultiplier(day, from, to);
            if (resultRef.isPresent())
                return resultRef;
        }

        return fromNetToRepo(loader, day, other, mainUnit, from, to, tasksBuilder);
    }

    private Optional<BigDecimal> fromNetToRepo(
            ExchangeRatesLoader loader, final UtcDay day, CurrencyUnit other, CurrencyUnit mainUnit, final CurrencyUnit from, final CurrencyUnit to, ImmutableList.Builder<Runnable> tasksBuilder
    ) {
        final Optional<BigDecimal> result = loadResult(loader, day, other, mainUnit, from, to, tasksBuilder);
        result.ifPresent(
                bigDecimal -> tasksBuilder.add(
                        () -> ratesRepository.addRate(day, from, to, bigDecimal)
                )
        );
        return result;
    }

    private Optional<BigDecimal> loadResult(
            ExchangeRatesLoader loader, UtcDay day, CurrencyUnit other, CurrencyUnit mainUnit, CurrencyUnit from, CurrencyUnit to, ImmutableList.Builder<Runnable> tasksBuilder
    ) {
        final Map<CurrencyUnit, BigDecimal> btcRates = loadCurrencies(loader, day, other);
        return checkOtherWayAroundAndGet(day, mainUnit, btcRates, from, to, tasksBuilder);
    }

    private static Map<CurrencyUnit, BigDecimal> loadCurrencies(ExchangeRatesLoader loader, UtcDay day, CurrencyUnit other) {
        return loader.loadCurrencies(Optional.of(day), Optional.of(ImmutableList.of(other)));
    }

    private Optional<BigDecimal> checkOtherWayAroundAndGet(
            final UtcDay day, final CurrencyUnit forRates, Map<CurrencyUnit, BigDecimal> rates, CurrencyUnit from, CurrencyUnit to, final ImmutableList.Builder<Runnable> tasksBuilder
    ) {
        if (rates.isEmpty())
            return Optional.empty();

        for (final Map.Entry<CurrencyUnit, BigDecimal> entry : rates.entrySet()) {
            accounter.streamRememberedBenefits(day, forRates, entry.getKey()).forEach(postponedMutationEvent -> tasksBuilder.add(() -> {
                final FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, this);
                core.setEvent(postponedMutationEvent.mutationEvent);
                core.setAmount(postponedMutationEvent.mutationEvent.amount);
                core.setDirection(FundsMutator.MutationDirection.BENEFIT);
                core.setConversionUnit(postponedMutationEvent.conversionUnit);
                core.setCustomRate(postponedMutationEvent.customRate);
                core.setNaturalRate(postponedMutationEvent.conversionUnit.equals(forRates) ? entry.getValue() : CurrencyRatesProvider.reverseRate(entry.getValue()));
                core.register();
            }));
            accounter.streamRememberedLosses(day, forRates, entry.getKey()).forEach(postponedMutationEvent -> tasksBuilder.add(() -> {
                final FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, this);
                core.setEvent(postponedMutationEvent.mutationEvent);
                core.setAmount(postponedMutationEvent.mutationEvent.amount);
                core.setDirection(FundsMutator.MutationDirection.LOSS);
                core.setConversionUnit(postponedMutationEvent.conversionUnit);
                core.setCustomRate(postponedMutationEvent.customRate);
                core.setNaturalRate(postponedMutationEvent.conversionUnit.equals(forRates) ? CurrencyRatesProvider.reverseRate(entry.getValue()) : entry.getValue());
                core.register();
            }));
            accounter.streamRememberedExchanges(day, forRates, entry.getKey()).forEach(postponedExchange -> tasksBuilder.add(() -> {
                final ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, this);
                core.setBuyAmount(postponedExchange.toBuy);
                core.setSellUnit(postponedExchange.unitSell);
                core.setCustomRate(postponedExchange.customRate.orElse(null));
                core.setNaturalRate(postponedExchange.unitSell.equals(forRates) ? CurrencyRatesProvider.reverseRate(entry.getValue()) : entry.getValue());
                core.submit();
            }));
        }

        if (from.equals(forRates)) {
            return Optional.ofNullable(rates.get(to));
        } else {
            final BigDecimal divisor = rates.get(from);
            if (divisor == null)
                return Optional.empty();
            return Optional.of(CurrencyRatesProvider.reverseRate(divisor));
        }
    }

}
