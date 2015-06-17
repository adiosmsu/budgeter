package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Date: 6/17/15
 * Time: 7:50 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrenciesExchangeServiceTest {

    private final CurrencyRatesRepositoryMock ratesRepository = new CurrencyRatesRepositoryMock();
    private final TreasuryMock treasury = new TreasuryMock();
    private CurrenciesExchangeService service = new CurrenciesExchangeService(
            new TransactionalSupportMock(),
            ratesRepository,
            new AccounterMock(),
            treasury,
            ExchangeRatesLoader.createBtcLoader(treasury),
            ExchangeRatesLoader.createCbrLoader(treasury)
    );

    @Test
    public void testGetConversionMultiplier() throws Exception {
        treasury.clear();
        ratesRepository.clear();

        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        treasury.registerCurrency(rub);
        treasury.registerCurrency(CurrencyUnit.USD);
        treasury.registerCurrency(CurrencyUnit.EUR);

        Thread.sleep(100);

        final UtcDay yesterday = new UtcDay(new UtcDay().inner.minus(1, ChronoUnit.DAYS));
        final Optional<BigDecimal> rubToUsd = service.getConversionMultiplier(yesterday, rub, CurrencyUnit.USD);
        Thread.sleep(100);
        assertTrue("rubToUsd didn't download from net", rubToUsd.isPresent());
        System.out.println("rubToUsd: " + rubToUsd.get());
        final Optional<BigDecimal> rubToEur = service.getConversionMultiplier(yesterday, rub, CurrencyUnit.EUR);
        Thread.sleep(100);
        assertTrue("rubToEur didn't download from net", rubToEur.isPresent());
        System.out.println("rubToEur: " + rubToEur.get());
        final Optional<BigDecimal> usdToEur = service.getConversionMultiplier(yesterday, CurrencyUnit.USD, CurrencyUnit.EUR);
        assertTrue("usdToEur didn't compute or download", usdToEur.isPresent());
        assertEquals("usdToEur didn't compute right (perhaps downloaded)",
                CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(rubToUsd.get(), rubToEur.get()), usdToEur.get());
        System.out.println("usdToEur: " + usdToEur.get());

        final Optional<BigDecimal> usdToRub = service.getConversionMultiplier(yesterday, CurrencyUnit.USD, rub);
        assertTrue("usdToRub didn't compute or download", usdToRub.isPresent());
        assertEquals(CurrencyRatesProvider.reverseRate(rubToUsd.get()), usdToRub.get());
        System.out.println("usdToRub: " + usdToRub.get());
    }

    @Test
    public void testProcessAllPostponedEvents() throws Exception {

    }

}