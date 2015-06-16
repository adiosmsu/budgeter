package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
        CurrencyUnit rub = CurrencyUnit.of("RUB");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger(0);
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
                CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, unit, BigDecimal.ONE);
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);

        final ImmutableSet<Integer> integers = CurrencyRatesPseudoTable.INSTANCE.getDayIndex().get(new UtcDay());
        final int i = CurrencyRatesPseudoTable.INSTANCE.idSequence.get();
        assertEquals(ImmutableSet.of(i, i-1, i-2, i-3, i-4, i-5, i-6, i-7, i-8, i-9), integers);

        try {
            CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, REG_UNITS.get(0), BigDecimal.ONE);
            fail("Double insert passed");
        } catch (Exception ignored) {}
        try {
            CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), REG_UNITS.get(0), rub, BigDecimal.ONE);
            fail("Double insert passed");
        } catch (Exception ignored) {}
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        CurrencyUnit rub = CurrencyUnit.of("RUB");
        final BigDecimal two = new BigDecimal(BigInteger.valueOf(2), 0, new MathContext(1));
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(ts), rub, CurrencyUnit.USD, two);
        assertEquals(two, CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(ts), rub, CurrencyUnit.USD).get());
        assertEquals(CurrencyRatesProvider.reverseRate(two), CurrencyRatesPseudoTable.INSTANCE.getConversionMultiplier(new UtcDay(ts), CurrencyUnit.USD, rub).get());
    }

    @Test
    public void testGetLatestOptionalConversionMultiplier() throws Exception {
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), CurrencyUnit.EUR, CurrencyUnit.USD, BigDecimal.valueOf(1234));
        assertEquals(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(1234)),
                CurrencyRatesPseudoTable.INSTANCE.getLatestOptionalConversionMultiplier(CurrencyUnit.USD, CurrencyUnit.EUR).get());
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
        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        final BigDecimal usdRate = BigDecimal.valueOf(55);
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, CurrencyUnit.USD, usdRate);
        final BigDecimal eurRate = BigDecimal.valueOf(65);
        CurrencyRatesPseudoTable.INSTANCE.addRate(new UtcDay(), rub, CurrencyUnit.EUR, eurRate);
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