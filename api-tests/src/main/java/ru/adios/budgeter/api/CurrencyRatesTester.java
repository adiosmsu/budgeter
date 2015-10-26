package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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

    public static final List<CurrencyUnit> REG_UNITS = CurrencyUnit.registeredCurrencies();

    private final Bundle bundle;

    public CurrencyRatesTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testAddRate() throws Exception {
        bundle.clear(Bundle.Repo.CURRENCY_RATES);

        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        CurrencyUnit rub = Units.RUB;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger(0);
        final CopyOnWriteArraySet<CurrencyUnit> checker = new CopyOnWriteArraySet<>();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail("Concurrent interruption fail");
                }
                CurrencyUnit unit = REG_UNITS.get(counter.getAndIncrement());
                if (unit.equals(rub))
                    unit = REG_UNITS.get(counter.getAndIncrement());
                checker.add(unit);
                ratesRepository.addRate(new UtcDay(), rub, unit, BigDecimal.ONE);
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);

        final ImmutableSet<Long> indexed = ratesRepository.getIndexedForDay(new UtcDay());
        indexed.stream().forEach(id -> {
            final CurrencyRatesProvider.ConversionRate conversionRate = ratesRepository.getById(id).get();
            assertTrue(conversionRate.pair.to + " indexed by mistake", checker.contains(conversionRate.pair.to));
        });
        for (CurrencyUnit unit : checker) {
            assertTrue("Added to index element " + unit + " not found in index",
                    indexed.stream().map(ratesRepository::getById).map(Optional::get).filter(rate -> rate.pair.to.equals(unit)).findFirst().isPresent());
        }

        assertFalse("Double insert passed", ratesRepository.addRate(new UtcDay(), Units.RUB, REG_UNITS.get(0), BigDecimal.ONE));

        try {
            ratesRepository.addRate(new UtcDay(), REG_UNITS.get(0), Units.RUB, BigDecimal.ONE);
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
        assertEquals(CurrencyRatesProvider.reverseRate(two), ratesRepository.getConversionMultiplier(new UtcDay(ts), CurrencyUnit.USD, Units.RUB).get());
        final Optional<BigDecimal> none =
                ratesRepository.getConversionMultiplier(new UtcDay(OffsetDateTime.of(1989, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.CHF);
        assertFalse(none.isPresent());
    }

    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();

        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.USD, BigDecimal.valueOf(1234));
        assertEquals(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(1234)),
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
        assertEquals(CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(usdRate, eurRate),
                ratesRepository.getLatestConversionMultiplier(CurrencyUnit.USD, CurrencyUnit.EUR));
    }

    public void testStreamConversionPairs() throws Exception {
        CurrencyRatesProvider.streamConversionPairs(ImmutableSet.of(CurrencyUnit.USD, CurrencyUnit.EUR, CurrencyUnit.CAD)).forEach(conversionPair -> {
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
        });
    }

}
