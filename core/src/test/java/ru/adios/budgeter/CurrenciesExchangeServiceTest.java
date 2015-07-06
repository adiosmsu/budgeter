package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private final AccounterMock accounter = new AccounterMock();
    private CurrenciesExchangeService service = new CurrenciesExchangeService(
            new TransactionalSupportMock(),
            ratesRepository,
            accounter,
            treasury,
            ExchangeRatesLoader.createBtcLoader(treasury),
            ExchangeRatesLoader.createCbrLoader(treasury)
    );

    @Before
    public void setUp() {
        treasury.clear();
        ratesRepository.clear();
        accounter.clear();

        treasury.registerCurrency(Units.RUB);
        treasury.registerCurrency(CurrencyUnit.USD);
        treasury.registerCurrency(CurrencyUnit.EUR);
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        Thread.sleep(100);

        final Optional<BigDecimal> rubToUsd = service.getConversionMultiplier(TestUtils.YESTERDAY, Units.RUB, CurrencyUnit.USD);
        Thread.sleep(100);
        assertTrue("rubToUsd didn't download from net", rubToUsd.isPresent());
        System.out.println("rubToUsd: " + rubToUsd.get());
        final Optional<BigDecimal> rubToEur = service.getConversionMultiplier(TestUtils.YESTERDAY, Units.RUB, CurrencyUnit.EUR);
        Thread.sleep(100);
        assertTrue("rubToEur didn't download from net", rubToEur.isPresent());
        System.out.println("rubToEur: " + rubToEur.get());
        final Optional<BigDecimal> usdToEur = service.getConversionMultiplier(TestUtils.YESTERDAY, CurrencyUnit.USD, CurrencyUnit.EUR);
        assertTrue("usdToEur didn't compute or download", usdToEur.isPresent());
        assertEquals("usdToEur didn't compute right (perhaps downloaded)",
                CurrencyRatesProvider.getConversionMultiplierFromIntermediateMultipliers(rubToUsd.get(), rubToEur.get()), usdToEur.get());
        System.out.println("usdToEur: " + usdToEur.get());

        final Optional<BigDecimal> usdToRub = service.getConversionMultiplier(TestUtils.YESTERDAY, CurrencyUnit.USD, Units.RUB);
        assertTrue("usdToRub didn't compute or download", usdToRub.isPresent());
        assertEquals(CurrencyRatesProvider.reverseRate(rubToUsd.get()), usdToRub.get());
        System.out.println("usdToRub: " + usdToRub.get());

        final BigDecimal ourVal = BigDecimal.valueOf(55.5534);
        ratesRepository.addRate(TestUtils.TODAY, Units.RUB, CurrencyUnit.USD, ourVal);
        final Optional<BigDecimal> ourRate = service.getConversionMultiplier(TestUtils.TODAY, Units.RUB, CurrencyUnit.USD);
        assertTrue("Today's rubToEur didn't compute or download", ourRate.isPresent());
        assertEquals(ourVal, ourRate.get());

        final Optional<BigDecimal> btcToRub = service.getConversionMultiplier(TestUtils.TODAY, Units.BTC, Units.RUB);
        assertTrue("Today's btcToRub didn't compute or download", btcToRub.isPresent());
        System.out.println("btcToRub: " + btcToRub.get());

        final Optional<BigDecimal> btcToRubYesterday = service.getConversionMultiplier(TestUtils.YESTERDAY, Units.BTC, Units.RUB);
        assertTrue("btcToRubYesterday didn't compute or download", btcToRubYesterday.isPresent());
        System.out.println("btcToRubYesterday: " + btcToRubYesterday.get());

        final Optional<BigDecimal> pastRate = service.getConversionMultiplier(new UtcDay(OffsetDateTime.of(2015, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)), Units.BTC, Units.RUB);
        assertTrue("btc pastRate didn't compute or download", pastRate.isPresent());
        System.out.println("btc pastRate: " + pastRate.get());
    }

    @Test
    public void testProcessAllPostponedEvents() throws Exception {
        /*TODO: write CORE classes tests first
        final FundsMutationSubjectRepository subjRepo = accounter.fundsMutationSubjectRepo();
        final FundsMutationSubject jobSubj = FundsMutationSubject.builder(subjRepo).setType(FundsMutationSubject.SubjectType.SERVICE).setName("Job").build();
        subjRepo.addSubject(jobSubj);

        accounter.rememberPostponedExchange(Money.of(Units.RUB, BigDecimal.valueOf(60000)), CurrencyUnit.EUR, Optional.of(BigDecimal.valueOf(60.0)), YESTERDAY.inner);
        accounter.rememberPostponedExchangeableBenefit(
                FundsMutationEvent.builder().setAmount(Money.of(Units.RUB, 110000.0)).setQuantity(1).setSubject(jobSubj).setTimestamp(TODAY.inner).build(),
                CurrencyUnit.EUR,
                Optional.empty()
        );

        ratesRepository.addRate(YESTERDAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(62.0));
        ratesRepository.addRate(TODAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(61.0));

        service.processAllPostponedEvents();

        final Optional<FundsMutationEvent> todayFirst = accounter.streamMutationsForDay(TODAY).findFirst();
        assertTrue("No remembered mutation event", todayFirst.isPresent());

        final Optional<CurrencyExchangeEvent> yesterdayFirst = accounter.streamExchangesForDay(YESTERDAY).findFirst();
        assertTrue("No remembered exchange event", yesterdayFirst.isPresent());
        */
    }

}