package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.adios.budgeter.api.*;

import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Date: 6/14/15
 * Time: 6:01 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrenciesExchangeService implements CurrencyRatesProvider {

    private static final Logger logger = LoggerFactory.getLogger(CurrenciesExchangeService.class);

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "currenciesExecutorThread");
        thread.setDaemon(true);
        return thread;
    });

    static {
        executor.submit(() -> logger.info("Executor started"));
    }

    @Autowired private volatile CurrencyRatesRepository ratesRepository;
    @Autowired private volatile Accounter accounter;
    @Autowired private volatile Treasury treasury;
    @Autowired private volatile ExchangeRatesLoader.BtcLoader btcLoader;
    @Autowired private volatile ExchangeRatesLoader.CbrLoader cbrLoader;

    private final Optional<TransactionalSupport> nonSpringTransactional;

    public CurrenciesExchangeService() {
        nonSpringTransactional = Optional.empty();
    }

    public CurrenciesExchangeService(
            TransactionalSupport nonSpringTransactional,
            CurrencyRatesRepository ratesRepository,
            Accounter accounter,
            Treasury treasury,
            ExchangeRatesLoader.BtcLoader btcLoader,
            ExchangeRatesLoader.CbrLoader cbrLoader
    ) {
        this.ratesRepository = ratesRepository;
        this.accounter = accounter;
        this.treasury = treasury;
        this.btcLoader = btcLoader;
        this.cbrLoader = cbrLoader;
        this.nonSpringTransactional = Optional.of(nonSpringTransactional);
    }

    @Transactional
    public void runWithTransaction(final ImmutableList<Runnable> runnables) {
        if (nonSpringTransactional.isPresent()) {
            nonSpringTransactional.get().runWithTransaction(() -> runRunnables(runnables));
        } else {
            runRunnables(runnables);
        }
    }

    private void runRunnables(ImmutableList<Runnable> runnables) {
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

    @Override
    public final Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(day, from, to);
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
                return Optional.of(CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(rubToFrom.get(), rubToTo.get())); // TODO: process postponed for arbitrary pair
            }

            return Optional.empty(); // nothing worked :(
        } finally {
            final ImmutableList<Runnable> tasks = tasksBuilder.build();
            if (tasks.size() > 0)
                executor.submit(() -> runWithTransaction(tasks.reverse()));
        }
    }

    //TODO: addRate method to add custom rates

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
        return ratesRepository.isRateStale(to);
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
        final Map<CurrencyUnit, BigDecimal> rates = loadCurrencies(loader, day, other);
        return checkOtherWayAroundAndGet(day, mainUnit, rates, from, to, tasksBuilder);
    }

    private static Map<CurrencyUnit, BigDecimal> loadCurrencies(ExchangeRatesLoader loader, UtcDay day, CurrencyUnit other) {
        loader.updateSupportedCurrencies();
        final Optional<List<CurrencyUnit>> problematicsRef = loader.isFetchingAllSupportedProblematic(day)
                ? Optional.of(ImmutableList.of(other))
                : Optional.empty();
        return loader.loadCurrencies(false, Optional.of(day), problematicsRef);
    }

    private Optional<BigDecimal> checkOtherWayAroundAndGet(
            final UtcDay day, final CurrencyUnit forRates, final Map<CurrencyUnit, BigDecimal> rates, CurrencyUnit from, CurrencyUnit to, final ImmutableList.Builder<Runnable> tasksBuilder
    ) {
        if (rates.isEmpty())
            return Optional.empty();

        tasksBuilder.add(() -> {
            for (final Map.Entry<CurrencyUnit, BigDecimal> entry : rates.entrySet()) {
                final BigDecimal rate = entry.getValue();
                final BigDecimal rateReversed = CurrencyRatesProvider.reverseRate(rate);
                final CurrencyUnit toUnit = entry.getKey();
                accounter.streamRememberedBenefits(day, forRates, toUnit).forEach(postponedMutationEvent -> {
                    final FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, this);
                    core.setPostponedEvent(postponedMutationEvent, postponedMutationEvent.conversionUnit.equals(forRates) ? rate : rateReversed);
                    core.submit();
                });
                accounter.streamRememberedLosses(day, forRates, toUnit).forEach(postponedMutationEvent -> {
                    final FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, this);
                    core.setPostponedEvent(postponedMutationEvent, postponedMutationEvent.conversionUnit.equals(forRates) ? rate : rateReversed);
                    core.submit();
                });
                accounter.streamRememberedExchanges(day, forRates, toUnit).forEach(postponedExchange -> {
                    final ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, this);
                    core.setPostponedEvent(postponedExchange, postponedExchange.unitSell.equals(forRates) ? rate : rateReversed);
                    core.submit();
                });
            }
        });

        try {
            if (from.equals(forRates)) {
                return Optional.ofNullable(rates.remove(to));
            } else {
                final BigDecimal divisor = rates.remove(from);
                if (divisor == null)
                    return Optional.empty();
                return Optional.of(CurrencyRatesProvider.reverseRate(divisor));
            }
        } finally {
            for (final Map.Entry<CurrencyUnit, BigDecimal> entry : rates.entrySet()) {
                tasksBuilder.add(() -> ratesRepository.addRate(day, forRates, entry.getKey(), entry.getValue()));
            }
        }
    }

}
