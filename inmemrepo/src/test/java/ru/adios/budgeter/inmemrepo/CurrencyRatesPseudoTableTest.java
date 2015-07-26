package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.UtcDay;

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
                CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, unit, BigDecimal.ONE);
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);

        final ImmutableSet<Integer> indexed = CurrencyRatesPseudoTable.INSTANCE.getDayIndex().get(new UtcDay());
        indexed.stream().forEach(id -> {
            final StoredCurrencyRate storedCurrencyRate = CurrencyRatesPseudoTable.INSTANCE.get(id);
            assertTrue(storedCurrencyRate.second + " indexed by mistake", checker.contains(storedCurrencyRate.second));
        });
        for (CurrencyUnit unit : checker) {
            assertTrue("Added to index element " + unit + " not found in index",
                    indexed.stream().map(CurrencyRatesPseudoTable.INSTANCE::get).filter(rate -> rate.second.equals(unit)).findFirst().isPresent());
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
        assertEquals(CurrencyRatesProvider.reverseRate(two), CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(ts), CurrencyUnit.USD, Units.RUB).get());
        final Optional<BigDecimal> none =
                CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(OffsetDateTime.of(1989, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.CHF);
        assertFalse(none.isPresent());
    }

    @Test
    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.USD, BigDecimal.valueOf(1234));
        assertEquals(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(1234)),
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
        assertEquals(CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(usdRate, eurRate),
                CurrencyRatesPseudoTable.INSTANCE.getLatestConversionMultiplier(CurrencyUnit.USD, CurrencyUnit.EUR));
    }

    @Test
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