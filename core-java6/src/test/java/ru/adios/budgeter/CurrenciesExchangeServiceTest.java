package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Date: 6/17/15
 * Time: 7:50 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrenciesExchangeServiceTest {

    public static final class State {

        private final Bundle bundle;

        private final CurrencyRatesRepository ratesRepository;
        private final Treasury treasury;
        private final Accounter accounter;
        private CurrenciesExchangeService service;

        private BalanceAccount accountRub;
        private BalanceAccount accountEur;

        public State(Bundle bundle) {
            this.bundle = bundle;
            ratesRepository = bundle.currencyRates();
            treasury = bundle.treasury();
            accounter = bundle.accounter();
            service = new CurrenciesExchangeService(
                    bundle.getTransactionalSupport(),
                    ratesRepository,
                    accounter,
                    treasury,
                    ExchangeRatesLoader.createBtcLoader(treasury),
                    ExchangeRatesLoader.createCbrLoader(treasury)
            );
        }

        public void setUp() {
            bundle.clearSchema();

            accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
            TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
            accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        }

    }

    private final State innerState = new State(Schema.INSTANCE);
    private final State jdbcState = new State(TestUtils.JDBC_BUNDLE);

    @Before
    public void setUp() {
        innerState.setUp();
        jdbcState.setUp();
    }

    @Test
    public void testGetConversionMultiplier() throws Exception {
        testGetConversionMultiplierWith(innerState, TestUtils.CASE_INNER);
        testGetConversionMultiplierWith(jdbcState, TestUtils.CASE_JDBC);
    }

    private void testGetConversionMultiplierWith(State state, String caseName) throws Exception {
        caseName += ": ";
        Thread.sleep(100);
        final MathContext mc = new MathContext(7, RoundingMode.HALF_DOWN);

        final Optional<BigDecimal> rubToUsd = state.service.getConversionMultiplier(TestUtils.YESTERDAY, Units.RUB, CurrencyUnit.USD);
        Thread.sleep(100);
        assertTrue(caseName + "rubToUsd didn't download from net", rubToUsd.isPresent());
        System.out.println(caseName + "rubToUsd: " + rubToUsd.get());
        assertTrue(caseName + "rubToUsd shows ruble stronger than dollar LOL", rubToUsd.get().compareTo(BigDecimal.ONE) < 0);
        final Optional<BigDecimal> rubToEur = state.service.getConversionMultiplier(TestUtils.YESTERDAY, Units.RUB, CurrencyUnit.EUR);
        Thread.sleep(100);
        assertTrue(caseName + "rubToEur didn't download from net", rubToEur.isPresent());
        System.out.println(caseName + "rubToEur: " + rubToEur.get());
        assertTrue(caseName + "rubToEur shows ruble stronger than euro LOL", rubToEur.get().compareTo(BigDecimal.ONE) < 0);
        final Optional<BigDecimal> usdToEur = state.service.getConversionMultiplier(TestUtils.YESTERDAY, CurrencyUnit.USD, CurrencyUnit.EUR);
        assertTrue(caseName + "usdToEur didn't compute or download", usdToEur.isPresent());
        assertEquals(caseName + "usdToEur didn't compute right (perhaps downloaded)",
                CurrencyRatesProvider.Static.getConversionMultiplierFromIntermediateMultipliers(rubToUsd.get(), rubToEur.get()).round(mc), usdToEur.get().round(mc));
        System.out.println("usdToEur: " + usdToEur.get());

        final Optional<BigDecimal> usdToRub = state.service.getConversionMultiplier(TestUtils.YESTERDAY, CurrencyUnit.USD, Units.RUB);
        assertTrue(caseName + "usdToRub didn't compute or download", usdToRub.isPresent());
        assertEquals(CurrencyRatesProvider.Static.reverseRate(rubToUsd.get()).round(mc).stripTrailingZeros(), usdToRub.get().round(mc).stripTrailingZeros());
        System.out.println(caseName + "usdToRub: " + usdToRub.get());

        final BigDecimal ourVal = BigDecimal.valueOf(55.5534);
        state.ratesRepository.addRate(TestUtils.TODAY, Units.RUB, CurrencyUnit.USD, ourVal);
        final Optional<BigDecimal> ourRate = state.service.getConversionMultiplier(TestUtils.TODAY, Units.RUB, CurrencyUnit.USD);
        assertTrue(caseName + "Today's rubToEur didn't compute or download", ourRate.isPresent());
        assertEquals(ourVal, ourRate.get());

        final Optional<BigDecimal> btcToRub = state.service.getConversionMultiplier(TestUtils.TODAY, Units.BTC, Units.RUB);
        assertTrue(caseName + "Today's btcToRub didn't compute or download", btcToRub.isPresent());
        System.out.println(caseName + "btcToRub: " + btcToRub.get());
        assertTrue(caseName + "btcToRub shows Bitcoin cheaper than ruble LOL", btcToRub.get().compareTo(BigDecimal.ONE) > 0);

        final Optional<BigDecimal> btcToRubYesterday = state.service.getConversionMultiplier(TestUtils.YESTERDAY, Units.BTC, Units.RUB);
        assertTrue(caseName + "btcToRubYesterday didn't compute or download", btcToRubYesterday.isPresent());
        System.out.println(caseName + "btcToRubYesterday: " + btcToRubYesterday.get());

        final Optional<BigDecimal> pastRate = state.service.getConversionMultiplier(new UtcDay(OffsetDateTime.of(2015, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)), Units.BTC, Units.RUB);
        assertTrue(caseName + "btc pastRate didn't compute or download", pastRate.isPresent());
        System.out.println(caseName + "btc pastRate: " + pastRate.get());
    }

    @Test
    public void testAddRates() throws Exception {
        testAddRatesWith(innerState, TestUtils.CASE_INNER);
        testAddRatesWith(jdbcState, TestUtils.CASE_JDBC);
    }

    private void testAddRatesWith(final State state, final String caseName) throws Exception {
        TestCheckedRunnable checkedRunnable = new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                prepareForPostponed(state);

                state.service.addRate(TestUtils.YESTERDAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(62.0));
                state.service.addRate(TestUtils.TODAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(61.0));
            }
        };
        jdbcState.bundle.tryExecuteInTransaction(checkedRunnable);

        Thread.sleep(100);

        checkedRunnable = new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                testPostponed(state, caseName + ": ");
            }
        };
        jdbcState.bundle.tryExecuteInTransaction(checkedRunnable);
    }

    @Test
    public void testProcessAllPostponedEventsWithManualRates() throws Exception {
        testProcessAllPostponedEventsWithManualRatesWith(innerState, TestUtils.CASE_INNER);
        testProcessAllPostponedEventsWithManualRatesWith(jdbcState, TestUtils.CASE_JDBC);
    }

    private void testProcessAllPostponedEventsWithManualRatesWith(State state, String caseName) throws Exception {
        caseName += ": ";
        prepareForPostponed(state);

        state.ratesRepository.addRate(TestUtils.YESTERDAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(62.0));
        state.ratesRepository.addRate(TestUtils.TODAY, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(61.0));

        final Integer[] tracker = new Integer[] {0};
        final CurrenciesExchangeService.ProcessPostponedResult processPostponedResult =
                state.service.processAllPostponedEvents(Optional.<Consumer<Integer>>of(
                        new Consumer<Integer>() {
                            @Override
                            public void accept(Integer percent) {
                                System.out.println("Postponed percentage: " + percent);
                                tracker[0] = percent;
                            }
                        }
                ), false);

        assertProcessPostponedResult(caseName, tracker[0], processPostponedResult);

        Thread.sleep(100);
        testPostponed(state, caseName);
    }

    private static void assertProcessPostponedResult(String caseName, Integer tracked, CurrenciesExchangeService.ProcessPostponedResult processPostponedResult) {
        assertEquals(caseName + "Percentage tracker failure", 100L, tracked.longValue());
        assertEquals(caseName + "Must be 2 succeeded conversion rates", 2, processPostponedResult.succeeded.size());
        assertTrue(caseName + "Conversion rate is not between RUB/EUR pair ",
                processPostponedResult.succeeded.get(0).pair.containsIgnoreOrder(CurrencyUnit.EUR, Units.RUB));
        assertTrue(caseName + "There are failed pairs", processPostponedResult.failed.isEmpty());
    }

    @Test
    public void testProcessAllPostponedEventsWithRequest() throws Exception {
        testProcessAllPostponedEventsWithRequestWith(innerState, TestUtils.CASE_INNER);
        testProcessAllPostponedEventsWithRequestWith(jdbcState, TestUtils.CASE_JDBC);
    }

    private void testProcessAllPostponedEventsWithRequestWith(State state, String caseName) throws Exception {
        caseName += ": ";
        prepareForPostponed(state);

        final Integer[] tracker = new Integer[] {0};
        final CurrenciesExchangeService.ProcessPostponedResult processPostponedResult =
                state.service.processAllPostponedEvents(Optional.<Consumer<Integer>>of(
                        new Consumer<Integer>() {
                            @Override
                            public void accept(Integer percent) {
                                tracker[0] = percent;
                            }
                        }
                ), false);

        assertProcessPostponedResult(caseName, tracker[0], processPostponedResult);

        Thread.sleep(100);
        testPostponed(state, caseName);
    }

    private void prepareForPostponed(State state) {
        final FundsMutationSubjectRepository subjRepo = state.accounter.fundsMutationSubjectRepo();
        final FundsMutationSubject jobSubj = subjRepo.addSubject(FundsMutationSubject.builder(subjRepo).setType(FundsMutationSubject.Type.SERVICE).setName("Job").build());

        final FundsMutationAgent testAgent = state.accounter.fundsMutationAgentRepo().addAgent(FundsMutationAgent.builder().setName("Test").build());

        state.accounter.postponedCurrencyExchangeEventRepository()
                .rememberPostponedExchange(
                        BigDecimal.valueOf(60000), state.accountRub, state.accountEur, Optional.of(BigDecimal.valueOf(60.0)), TestUtils.YESTERDAY.inner, testAgent
                );
        state.accounter.postponedFundsMutationEventRepository().rememberPostponedExchangeableEvent(
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.RUB, 110000.0))
                        .setRelevantBalance(state.accountRub)
                        .setSubject(jobSubj)
                        .setTimestamp(TestUtils.TODAY.inner)
                        .setAgent(testAgent).build(),
                CurrencyUnit.EUR, Optional.<BigDecimal>empty()
        );
    }

    private void testPostponed(State state, String caseName) {
        final Optional<FundsMutationEvent> todayFirst = state.bundle.fundsMutationEvents().streamForDay(TestUtils.TODAY).findFirst();
        assertTrue(caseName + "No remembered mutation event", todayFirst.isPresent());

        final Optional<CurrencyExchangeEvent> yesterdayFirst = state.bundle.currencyExchangeEvents().streamForDay(TestUtils.YESTERDAY).findFirst();
        assertTrue(caseName + "No remembered exchange event", yesterdayFirst.isPresent());
    }

}