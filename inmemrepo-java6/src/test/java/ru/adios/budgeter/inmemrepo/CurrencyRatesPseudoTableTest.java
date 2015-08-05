package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.UtcDay;

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
 * Date: 6/15/15
 * Time: 8:14 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyRatesPseudoTableTest {

    public static final List<CurrencyUnit> REG_UNITS = CurrencyUnit.registeredCurrencies();

    @Test
    public void testAddRate() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.clear();

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
                    checker.add(unit);
                    CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, unit, BigDecimal.ONE);
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);

        final ImmutableSet<Integer> indexed = CurrencyRatesPseudoTable.INSTANCE.getDayIndex().get(new UtcDay());
        StreamSupport.stream(indexed).forEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer id) {
                final StoredCurrencyRate storedCurrencyRate = CurrencyRatesPseudoTable.INSTANCE.get(id);
                assertTrue(storedCurrencyRate.second + " indexed by mistake", checker.contains(storedCurrencyRate.second));
            }
        });
        for (final CurrencyUnit unit : checker) {
            assertTrue("Added to index element " + unit + " not found in index",
                    StreamSupport.stream(indexed).map(new Function<Integer, StoredCurrencyRate>() {
                        @Override
                        public StoredCurrencyRate apply(Integer integer) {
                            return CurrencyRatesPseudoTable.INSTANCE.get(integer);
                        }
                    }).filter(new Predicate<StoredCurrencyRate>() {
                        @Override
                        public boolean test(StoredCurrencyRate rate) {
                            return rate.second.equals(unit);
                        }
                    }).findFirst().isPresent());
        }

        assertFalse("Double insert passed", CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), Units.RUB, REG_UNITS.get(0), BigDecimal.ONE));

        try {
            CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), REG_UNITS.get(0), Units.RUB, BigDecimal.ONE);
        } catch (Exception ignored) {
            final StringWriter writer = new StringWriter();
            ignored.printStackTrace(new PrintWriter(writer));
            fail("Symmetric insert failed: " + writer.toString());
        }
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final BigDecimal two = new BigDecimal(BigInteger.valueOf(2), 0, new MathContext(1));
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(ts), Units.RUB, CurrencyUnit.USD, two);
        assertEquals(two, CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).get());
        assertEquals(CurrencyRatesProvider.Static.reverseRate(two), CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(ts), CurrencyUnit.USD, Units.RUB).get());
        final Optional<BigDecimal> none =
                CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(OffsetDateTime.of(1989, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.CHF);
        assertFalse(none.isPresent());
    }

    @Test
    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.USD, BigDecimal.valueOf(1234));
        assertEquals(CurrencyRatesProvider.Static.reverseRate(BigDecimal.valueOf(1234)),
                CurrencyRatesPseudoTable.INSTANCE.getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit.USD, CurrencyUnit.EUR).get());
    }

    @Test
    public void testIsRateStale() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.AUD, BigDecimal.valueOf(1234));
        assertFalse(CurrencyRatesPseudoTable.INSTANCE.isRateStale(CurrencyUnit.EUR));
        assertTrue(CurrencyRatesPseudoTable.INSTANCE.isRateStale(CurrencyUnit.of("ZWL")));
    }

    @Test
    public void testGetLatestConversionMultiplier() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.clear();
        final BigDecimal usdRate = BigDecimal.valueOf(55);
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), Units.RUB, CurrencyUnit.USD, usdRate);
        final BigDecimal eurRate = BigDecimal.valueOf(65);
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), Units.RUB, CurrencyUnit.EUR, eurRate);
        assertEquals(CurrencyRatesProvider.Static.getConversionMultiplierFromIntermediateMultipliers(usdRate, eurRate),
                CurrencyRatesPseudoTable.INSTANCE.getLatestConversionMultiplier(CurrencyUnit.USD, CurrencyUnit.EUR));
    }

    @Test
    public void testStreamConversionPairs() throws Exception {
        CurrencyRatesProvider.Static.streamConversionPairs(ImmutableSet.of(CurrencyUnit.USD, CurrencyUnit.EUR, CurrencyUnit.CAD)).forEach(new Consumer<CurrencyRatesProvider.ConversionPair>() {
            @Override
            public void accept(CurrencyRatesProvider.ConversionPair conversionPair) {
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