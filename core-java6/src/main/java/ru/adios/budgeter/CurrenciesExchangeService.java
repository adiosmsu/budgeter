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

package ru.adios.budgeter;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.ConversionPair;
import ru.adios.budgeter.api.data.ConversionRate;
import ru.adios.budgeter.api.data.PostponedExchange;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Date: 6/14/15
 * Time: 6:01 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrenciesExchangeService implements CurrencyRatesRepository {

    private static final Logger logger = LoggerFactory.getLogger(CurrenciesExchangeService.class);

    private static final class LazyExecutorHolder {

        private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@Nonnull Runnable r) {
                final Thread thread = new Thread(r, "currenciesExecutorThread");
                thread.setDaemon(true);
                return thread;
            }
        });

        static {
            EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("Executor started");
                }
            });
        }

    }

    @Autowired private volatile CurrencyRatesRepository ratesRepository;
    @Autowired private volatile Accounter accounter;
    @Autowired private volatile Treasury treasury;
    @Autowired private volatile ExchangeRatesLoader.BtcLoader btcLoader;
    @Autowired private volatile ExchangeRatesLoader.CbrLoader cbrLoader;

    private final AtomicReference<Executor> executorRef = new AtomicReference<Executor>(null);
    private final Optional<TransactionalSupport> nonSpringTransactional;

    private final CurrencyRatesRepository.Default repoDef = new CurrencyRatesRepository.Default(this);

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
        this.nonSpringTransactional = Optional.ofNullable(nonSpringTransactional);
    }

    public void setExecutor(Executor executor) {
        executorRef.set(executor);
    }

    public void executeInSameThread() {
        executorRef.set(MoreExecutors.directExecutor());
    }

    public Executor getExecutor() {
        final Executor executor = executorRef.get();
        if (executor == null) {
            executorRef.compareAndSet(null, LazyExecutorHolder.EXECUTOR);
        }
        return executorRef.get();
    }

    @Transactional
    public void runWithTransaction(final ImmutableList<Runnable> runnables) {
        if (nonSpringTransactional.isPresent()) {
            nonSpringTransactional.get().runWithTransaction(new Runnable() {
                @Override
                public void run() {
                    runRunnables(runnables);
                }
            });
        } else {
            runRunnables(runnables);
        }
    }

    private void runRunnables(ImmutableList<Runnable> runnables) {
        try {
            for (final Runnable r : runnables) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Running end-runnable: " + r);
                }
                r.run();
            }
        } catch (Throwable th) {
            logger.error("Execution of currencies exchange service tasks error", th);
            throw Throwables.propagate(th);
        }
    }

    @Override
    public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return repoDef.addTodayRate(from, to, rate);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getConversionMultiplierBidirectional(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return repoDef.getConversionMultiplierWithIntermediate(day, from, to, intermediate);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getLatestOptionalConversionMultiplierBidirectional(from, to);
    }

    @Override
    public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return repoDef.getLatestConversionMultiplierWithIntermediate(from, to, intermediate);
    }

    public static final class ProcessPostponedResult {
        public final ImmutableList<ConversionRate> succeeded;
        public final ImmutableList<ConversionRate> failed;

        private ProcessPostponedResult(ImmutableList<ConversionRate> succeeded, ImmutableList<ConversionRate> failed) {
            this.succeeded = succeeded;
            this.failed = failed;
        }
    }
    public final ProcessPostponedResult processAllPostponedEvents(final Optional<Consumer<Integer>> percentageProgressTracker, boolean compatMode) {
        final Integer[] counter = new Integer[] {0};
        final List<Accounter.PostponingReasons> collected =
                accounter.streamAllPostponingReasons(compatMode)
                        .peek(new Consumer<Accounter.PostponingReasons>() {
                            @Override
                            public void accept(Accounter.PostponingReasons postponingReasons) {
                                if (percentageProgressTracker.isPresent() && counter[0] < 20) {
                                    counter[0] += 2;
                                    percentageProgressTracker.get().accept(counter[0]);
                                }
                            }
                        })
                        .collect(Collectors.<Accounter.PostponingReasons>toList());

        float quantaSecondStage = 0f;
        int totalPercentage = counter[0];
        if (percentageProgressTracker.isPresent()) {
            if (totalPercentage < 20) {
                percentageProgressTracker.get().accept(totalPercentage = 20);
            }
            float totalSecondStage = 0;
            for (final Accounter.PostponingReasons reasons : collected) {
                totalSecondStage += reasons.sufferingUnits.size();
            }
            quantaSecondStage = 20f / totalSecondStage;
        }

        final HashMap<UtcDay, List<ConversionPair>> work = new HashMap<UtcDay, List<ConversionPair>>(collected.size() + 1, 1f);
        float tw = 0f;
        for (final Accounter.PostponingReasons reasons : collected) {
            final List<ConversionPair> pairsToLoad =
                    CurrencyRatesProvider.Static.streamConversionPairs(reasons.sufferingUnits).collect(Collectors.<ConversionPair>toList());

            work.put(reasons.dayUtc, pairsToLoad);

            if (percentageProgressTracker.isPresent()) {
                percentageProgressTracker.get().accept(totalPercentage += (int) (quantaSecondStage * reasons.sufferingUnits.size()));
                tw += pairsToLoad.size();
            }
        }

        float quantaLastStage = 0f;
        if (percentageProgressTracker.isPresent()) {
            if (totalPercentage < 40) {
                percentageProgressTracker.get().accept(totalPercentage = 40);
            }
            quantaLastStage = 60f / tw;
        }

        final ImmutableList.Builder<ConversionRate> successesBuilder = ImmutableList.builder();
        final ImmutableList.Builder<ConversionRate> failsBuilder = ImmutableList.builder();

        for (final Map.Entry<UtcDay, List<ConversionPair>> entry : work.entrySet()) {
            final UtcDay day = entry.getKey();
            final List<ConversionPair> pairsList = entry.getValue();

            for (ConversionPair conversionPair : pairsList) {
                final Optional<BigDecimal> multiplier = getConversionMultiplier(day, conversionPair.from, conversionPair.to, true);

                if (multiplier.isPresent()) {
                    successesBuilder.add(new ConversionRate(day, conversionPair, multiplier.get()));
                } else {
                    failsBuilder.add(new ConversionRate(day, conversionPair, BigDecimal.ZERO));
                }
            }

            if (percentageProgressTracker.isPresent()) {
                percentageProgressTracker.get().accept(totalPercentage += (int) (quantaLastStage * pairsList.size()));
            }
        }

        if (percentageProgressTracker.isPresent() && totalPercentage < 100) {
            percentageProgressTracker.get().accept(100);
        }

        return new ProcessPostponedResult(successesBuilder.build(), failsBuilder.build());
    }

    @Override
    public final Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(day, from, to, false);
    }

    @Override
    public final Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(day, from, to);
    }

    private Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to, boolean processPostponedForExistingRates) {
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
                    return loadFromNet(btcLoader, day, other, btcUnit, from, to, tasksBuilder, null);
                }

                return conversionMultiplierFor(btcLoader, day, other, btcUnit, from, to, tasksBuilder, processPostponedForExistingRates, null);
            }

            final CurrencyUnit rubUnit = cbrLoader.getMainUnit();
            if (from.equals(rubUnit) || to.equals(rubUnit)) {
                // rub case
                final CurrencyUnit other = from.equals(rubUnit)
                        ? to
                        : from;

                return conversionMultiplierFor(cbrLoader, day, other, rubUnit, from, to, tasksBuilder, processPostponedForExistingRates, null);
            }

            // arbitrary case
            final HashMap<CurrencyUnit, BigDecimal> rubCache = new HashMap<CurrencyUnit, BigDecimal>();
            final Optional<BigDecimal> rubToFrom = conversionMultiplierFor(cbrLoader, day, from, rubUnit, rubUnit, from, tasksBuilder, processPostponedForExistingRates, rubCache);
            Optional<BigDecimal> rubToTo = Optional.ofNullable(rubCache.get(to));
            if (!rubToTo.isPresent()) {
                rubToTo = conversionMultiplierFor(cbrLoader, day, to, rubUnit, rubUnit, to, tasksBuilder, processPostponedForExistingRates, null);
            }
            if (rubToFrom.isPresent() && rubToTo.isPresent()) {
                final BigDecimal arbitraryRate = CurrencyRatesProvider.Static.getConversionMultiplierFromIntermediateMultipliers(rubToFrom.get(), rubToTo.get());
                addPostponedTask(ImmutableMap.of(from, arbitraryRate), day, to, tasksBuilder);
                return Optional.of(arbitraryRate);
            }

            return Optional.empty(); // nothing worked :(
        } finally {
            scheduleTasks(tasksBuilder);
        }
    }

    private void scheduleTasks(ImmutableList.Builder<Runnable> tasksBuilder) {
        final ImmutableList<Runnable> tasks = tasksBuilder.build();
        if (tasks.size() > 0) {
            getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    runWithTransaction(tasks.reverse());
                }
            });
        }
    }

    @Override
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        final boolean success = ratesRepository.addRate(dayUtc, from, to, rate);

        if (success) {
            final ImmutableList.Builder<Runnable> tasksBuilder = new ImmutableList.Builder<Runnable>();
            try {
                addPostponedTask(ImmutableMap.of(from, rate), dayUtc, to, tasksBuilder);
            } finally {
                scheduleTasks(tasksBuilder);
            }
        }

        return success;
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
        return ratesRepository.isRateStale(to);
    }

    @Override
    public ImmutableSet<Long> getIndexedForDay(UtcDay day) {
        return ratesRepository.getIndexedForDay(day);
    }

    @Override
    public Long currentSeqValue() {
        return ratesRepository.currentSeqValue();
    }

    @Override
    public Optional<ConversionRate> getById(Long id) {
        return ratesRepository.getById(id);
    }

    private Optional<BigDecimal> conversionMultiplierFor(
            ExchangeRatesLoader loader,
            UtcDay day,
            CurrencyUnit other,
            CurrencyUnit mainUnit,
            CurrencyUnit from,
            CurrencyUnit to,
            ImmutableList.Builder<Runnable> tasksBuilder,
            boolean processPostponedForExistingRates,
            @Nullable HashMap<CurrencyUnit, BigDecimal> rubCache
    ) {
        final Optional<BigDecimal> resultRef = ratesRepository.getConversionMultiplier(day, from, to);
        if (resultRef.isPresent()) {
            if (processPostponedForExistingRates) {
                addPostponedTask(ImmutableMap.of(from, resultRef.get()), day, to, tasksBuilder);
            }
            return resultRef;
        }

        return fromNetToRepo(loader, day, other, mainUnit, from, to, tasksBuilder, rubCache);
    }

    private Optional<BigDecimal> fromNetToRepo(
            ExchangeRatesLoader loader,
            final UtcDay day,
            CurrencyUnit other,
            CurrencyUnit mainUnit,
            final CurrencyUnit from,
            final CurrencyUnit to,
            final ImmutableList.Builder<Runnable> tasksBuilder,
            @Nullable HashMap<CurrencyUnit, BigDecimal> rubCache
    ) {
        final Optional<BigDecimal> result = loadFromNet(loader, day, other, mainUnit, from, to, tasksBuilder, rubCache);
        result.ifPresent(
                new Consumer<BigDecimal>() {
                    @Override
                    public void accept(final BigDecimal bigDecimal) {
                        tasksBuilder.add(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            addRateToDelegate(day, from, to, bigDecimal);
                                        } catch (Throwable th) {
                                            logger.error("Rate addition (the one requested) after load from net failed", th);
                                        }
                                    }
                                }
                        );
                    }
                }
        );
        return result;
    }

    private Optional<BigDecimal> loadFromNet(
            ExchangeRatesLoader loader,
            UtcDay day,
            CurrencyUnit other,
            CurrencyUnit mainUnit,
            CurrencyUnit from,
            CurrencyUnit to,
            ImmutableList.Builder<Runnable> tasksBuilder,
            @Nullable final HashMap<CurrencyUnit, BigDecimal> rubCache
    ) {
        final Map<CurrencyUnit, BigDecimal> rates = loadCurrencies(loader, day, other);
        return checkOtherWayAroundAndGet(day, mainUnit, rates, from, to, tasksBuilder, loader.directionFromMainToMapped(), rubCache);
    }

    private static Map<CurrencyUnit, BigDecimal> loadCurrencies(ExchangeRatesLoader loader, UtcDay day, CurrencyUnit other) {
        loader.updateSupportedCurrencies();
        final Optional<List<CurrencyUnit>> problematicsRef;
        if (loader.isFetchingAllSupportedProblematic(day)) {
            problematicsRef = Optional.<List<CurrencyUnit>>of(ImmutableList.of(other));
        } else {
            problematicsRef = Optional.empty();
            loader.addToSupportedCurrencies(other);
        }
        return loader.loadCurrencies(false, Optional.of(day), problematicsRef);
    }

    private Optional<BigDecimal> checkOtherWayAroundAndGet(final UtcDay day,
                                                           final CurrencyUnit mainUnit,
                                                           final Map<CurrencyUnit, BigDecimal> rates,
                                                           CurrencyUnit from,
                                                           CurrencyUnit to,
                                                           final ImmutableList.Builder<Runnable> tasksBuilder,
                                                           final boolean directionFromMainToMapped,
                                                           @Nullable final HashMap<CurrencyUnit, BigDecimal> rubCache) {
        if (rates.isEmpty())
            return Optional.empty();
        final boolean btcVolatile = day.equals(new UtcDay()) && mainUnit.equals(Units.BTC);

        if (!btcVolatile) {
            addPostponedTask(rates, day, mainUnit, tasksBuilder);
        }

        final boolean fromIsMain = from.equals(mainUnit);
        final boolean doStraight = fromIsMain == directionFromMainToMapped;
        final BigDecimal ourRate = rates.remove(fromIsMain ? to : from);
        try {
            try {
                if (doStraight) {
                    return Optional.ofNullable(ourRate);
                } else {
                    if (ourRate == null)
                        return Optional.empty();
                    return Optional.of(CurrencyRatesProvider.Static.reverseRate(ourRate));
                }
            } finally {
                if (rubCache != null) {
                    for (final Map.Entry<CurrencyUnit, BigDecimal> entry : rates.entrySet()) {
                        rubCache.put(entry.getKey(), doStraight ? entry.getValue() : CurrencyRatesProvider.Static.reverseRate(entry.getValue()));
                    }
                }
            }
        } finally {
            if (!btcVolatile) {
                for (final Map.Entry<CurrencyUnit, BigDecimal> entry : rates.entrySet()) {
                    final CurrencyUnit key = entry.getKey();
                    tasksBuilder.add(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (directionFromMainToMapped) {
                                            addRateToDelegate(day, mainUnit, entry.getKey(), entry.getValue());
                                        } else {
                                            addRateToDelegate(day, entry.getKey(), mainUnit, entry.getValue());
                                        }
                                    } catch (Throwable th) {
                                        logger.error("Rate addition after load from net failed", th);
                                        Throwables.propagate(th);
                                    }
                                }
                            }
                    );
                }
            }
        }
    }

    private void addRateToDelegate(UtcDay day, CurrencyUnit from, CurrencyUnit to, BigDecimal bigDecimal) {
        if (!ratesRepository.addRate(day, from, to, bigDecimal)) {
            logger.error("Rate addition after load from net failed for some reason");
        }
    }

    private void addPostponedTask(Map<CurrencyUnit, BigDecimal> rates, final UtcDay day, final CurrencyUnit forRates, ImmutableList.Builder<Runnable> tasksBuilder) {
        final ImmutableMap<CurrencyUnit, BigDecimal> ratesSnapshot = ImmutableMap.copyOf(rates);
        tasksBuilder.add(new Runnable() {
            @Override
            public void run() {
                try {
                    for (final Map.Entry<CurrencyUnit, BigDecimal> entry : ratesSnapshot.entrySet()) {
                        final BigDecimal rate = entry.getValue();
                        final BigDecimal rateReversed = CurrencyRatesProvider.Static.reverseRate(rate);
                        final CurrencyUnit toUnit = entry.getKey();
                        accounter.postponedFundsMutationEventRepository()
                                .streamRememberedEvents(day, forRates, toUnit)
                                .forEach(new Consumer<PostponedMutationEvent>() {
                                    @Override
                                    public void accept(PostponedMutationEvent event) {
                                        final FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, CurrenciesExchangeService.this);
                                        core.setPostponedEvent(event, event.conversionUnit.equals(forRates) ? rate : rateReversed);
                                        final Submitter.Result res = core.submit();
                                        if (!res.isSuccessful()) {
                                            logger.info("Remembered events save fail; general error: {}; field errors: {}", res.generalError, Arrays.toString(res.fieldErrors.toArray()));
                                        }
                                    }
                                });
                        accounter.postponedCurrencyExchangeEventRepository()
                                .streamRememberedExchanges(day, forRates, toUnit)
                                .forEach(new Consumer<PostponedExchange>() {
                                    @Override
                                    public void accept(PostponedExchange postponedExchange) {
                                        final ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, CurrenciesExchangeService.this);
                                        core.setPostponedEvent(postponedExchange, postponedExchange.sellAccount.getUnit().equals(forRates) ? rate : rateReversed);
                                        final Submitter.Result res = core.submit();
                                        if (!res.isSuccessful()) {
                                            logger.info("Remembered exchanges save fail; general error: {}; field errors: {}", res.generalError, Arrays.toString(res.fieldErrors.toArray()));
                                        }
                                    }
                                });
                    }
                } catch (Throwable th) {
                    logger.error("Postponed tasks reenactment error", th);
                    Throwables.propagate(th);
                }
            }
        });
    }

}
