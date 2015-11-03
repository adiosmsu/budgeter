package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.data.ConversionPair;
import ru.adios.budgeter.api.data.ConversionRate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Date: 10/26/15
 * Time: 4:07 PM
 *
 * @author Mikhail Kulikov
 */
public final class CurrencyRatesTester {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyRatesTester.class);

    public static final List<CurrencyUnit> REG_UNITS = CurrencyUnit.registeredCurrencies();

    private final Bundle bundle;

    public CurrencyRatesTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testAddRate(long workerSleepMillis) throws Exception {
        bundle.clear(Bundle.Repo.CURRENCY_RATES);

        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        final CurrencyUnit rub = Units.RUB;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger(0);
        final CopyOnWriteArraySet<CurrencyUnit> checker = new CopyOnWriteArraySet<CurrencyUnit>();
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail("Concurrent interruption fail");
                    }
                    CurrencyUnit unit = REG_UNITS.get(counter.getAndIncrement());
                    if (unit.equals(rub))
                        unit = REG_UNITS.get(counter.getAndIncrement());
                    final CurrencyUnit unitFin = unit;
                    checker.add(unit);
                    final UtcDay dayUtc = new UtcDay();
                    final TransactionalSupport txSupport = bundle.getTransactionalSupport();
                    if (txSupport != null) {
                        logger.info("Firing transactional");
                        txSupport.runWithTransaction(new Runnable() {
                            @Override
                            public void run() {
                                ratesRepository.addRate(dayUtc, rub, unitFin, BigDecimal.ONE);
                            }
                        });
                    } else {
                        ratesRepository.addRate(dayUtc, rub, unit, BigDecimal.ONE);
                    }
                    logger.info("Added rate: {}, {}, {}, {}", dayUtc, rub, unit, BigDecimal.ONE);
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(workerSleepMillis);

        final ImmutableSet<Long> indexed = ratesRepository.getIndexedForDay(new UtcDay());
        StreamSupport.stream(indexed).forEach(new Consumer<Long>() {
            @Override
            public void accept(Long id) {
                final ConversionRate conversionRate = ratesRepository.getById(id).get();
                assertTrue(conversionRate.pair.to + " indexed by mistake", checker.contains(conversionRate.pair.to));
            }
        });
        for (final CurrencyUnit unit : checker) {
            assertTrue("Added to index element " + unit + " not found in index",
                    StreamSupport.stream(indexed).map(new Function<Long, ConversionRate>() {
                        @Override
                        public ConversionRate apply(Long id) {
                            return ratesRepository.getById(id).get();
                        }
                    }).filter(new Predicate<ConversionRate>() {
                        @Override
                        public boolean test(ConversionRate rate) {
                            return rate.pair.to.equals(unit);
                        }
                    }).findFirst().isPresent());
        }

        final TransactionalSupport txSupport = bundle.getTransactionalSupport();
        if (txSupport != null) {
            logger.info("Firing transactional");
            txSupport.runWithTransaction(new Runnable() {
                @Override
                public void run() {
                    testAddRateInner1(ratesRepository, checker);
                }
            });
            txSupport.runWithTransaction(new Runnable() {
                @Override
                public void run() {
                    testAddRateInner2(ratesRepository, checker);
                }
            });
        } else {
            testAddRateInner1(ratesRepository, checker);
            testAddRateInner2(ratesRepository, checker);
        }
    }

    private void testAddRateInner1(CurrencyRatesRepository ratesRepository, CopyOnWriteArraySet<CurrencyUnit> checker) {
        assertFalse("Double insert passed", ratesRepository.addRate(new UtcDay(), Units.RUB, REG_UNITS.get(0), BigDecimal.ONE));
    }

    private void testAddRateInner2(CurrencyRatesRepository ratesRepository, CopyOnWriteArraySet<CurrencyUnit> checker) {
        try {
            assertTrue(ratesRepository.addRate(new UtcDay(), REG_UNITS.get(0), Units.RUB, BigDecimal.ONE));
        } catch (Exception ignored) {
            final StringWriter writer = new StringWriter();
            ignored.printStackTrace(new PrintWriter(writer));
            fail("Symmetric insert failed: " + writer.toString());
        }
    }

    public void testGetConversionMultiplier() throws Exception {
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        final OffsetDateTime ts = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final BigDecimal two = new BigDecimal(BigInteger.valueOf(2), 0, new MathContext(1));
        ratesRepository.addRate(new UtcDay(ts), Units.RUB, CurrencyUnit.USD, two);
        assertEquals(two, ratesRepository.getConversionMultiplier(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).get());
        assertEquals(CurrencyRatesProvider.Static.reverseRate(two), ratesRepository.getConversionMultiplier(new UtcDay(ts), CurrencyUnit.USD, Units.RUB).get());
        final Optional<BigDecimal> none =
                ratesRepository.getConversionMultiplier(new UtcDay(OffsetDateTime.of(1989, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.CHF);
        assertFalse(none.isPresent());
    }

    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.USD, BigDecimal.valueOf(1234));
        assertEquals(CurrencyRatesProvider.Static.reverseRate(BigDecimal.valueOf(1234)),
                ratesRepository.getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit.USD, CurrencyUnit.EUR).get());
    }

    public void testIsRateStale() throws Exception {
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.AUD, BigDecimal.valueOf(1234));
        assertFalse(ratesRepository.isRateStale(CurrencyUnit.EUR));
        assertTrue(ratesRepository.isRateStale(CurrencyUnit.of("ZWL")));
    }

    public void testGetLatestConversionMultiplier() throws Exception {
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        bundle.clear(Bundle.Repo.CURRENCY_RATES);
        final BigDecimal usdRate = BigDecimal.valueOf(55);
        ratesRepository.addRate(new UtcDay(), Units.RUB, CurrencyUnit.USD, usdRate);
        final BigDecimal eurRate = BigDecimal.valueOf(65);
        ratesRepository.addRate(new UtcDay(), Units.RUB, CurrencyUnit.EUR, eurRate);
        assertEquals(CurrencyRatesProvider.Static.getConversionMultiplierFromIntermediateMultipliers(usdRate, eurRate),
                ratesRepository.getLatestConversionMultiplier(CurrencyUnit.USD, CurrencyUnit.EUR));
    }

    public void testStreamConversionPairs() throws Exception {
        CurrencyRatesProvider.Static.streamConversionPairs(ImmutableSet.of(CurrencyUnit.USD, CurrencyUnit.EUR, CurrencyUnit.CAD)).forEach(new Consumer<ConversionPair>() {
            @Override
            public void accept(ConversionPair conversionPair) {
                System.out.println(String.valueOf(conversionPair.from) + ", " + conversionPair.to);
                if (conversionPair.from.equals(CurrencyUnit.USD)) {
                    assertTrue(conversionPair.to.equals(CurrencyUnit.CAD) || conversionPair.to.equals(CurrencyUnit.EUR));
                }
                if (conversionPair.from.equals(CurrencyUnit.CAD)) {
                    assertTrue(conversionPair.to.equals(CurrencyUnit.USD) || conversionPair.to.equals(CurrencyUnit.EUR));
                }
                if (conversionPair.from.equals(CurrencyUnit.EUR)) {
                    assertTrue(conversionPair.to.equals(CurrencyUnit.USD) || conversionPair.to.equals(CurrencyUnit.CAD));
                }
            }
        });
    }

}
