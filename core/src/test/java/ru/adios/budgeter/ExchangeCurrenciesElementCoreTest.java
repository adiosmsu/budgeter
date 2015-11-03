package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Date: 03.07.15
 * Time: 22:30
 *
 * @author Mikhail Kulikov
 */
public class ExchangeCurrenciesElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        final TestCheckedRunnable runnable = () -> testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
        TestUtils.JDBC_BUNDLE.tryExecuteInTransaction(runnable);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        final MathContext mc = new MathContext(7, RoundingMode.HALF_DOWN);
        final Accounter accounter = bundle.accounter();
        final Treasury treasury = bundle.treasury();
        final CurrencyRatesRepository ratesRepo = bundle.currencyRates();
        final CurrenciesExchangeService ratesService =
                new CurrenciesExchangeService(bundle.getTransactionalSupport(),
                        ratesRepo, accounter, treasury, ExchangeRatesLoader.createBtcLoader(treasury), ExchangeRatesLoader.createCbrLoader(treasury));

        bundle.clearSchema();

        final FundsMutationAgent agentExchanger = accounter.fundsMutationAgentRepo().addAgent(FundsMutationAgent.builder().setName("Обменник").build());

        treasury.addAmount(Money.of(CurrencyUnit.USD, 300000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 1000), "eur");
        treasury.addAmount(Money.of(Units.RUB, 1000000), "rub");
        treasury.addAmount(Money.of(Units.BTC, 100), "btc");
        BalanceAccount usdAccount = treasury.getAccountWithId(new BalanceAccount("usd", CurrencyUnit.USD));
        BalanceAccount eurAccount = treasury.getAccountWithId(new BalanceAccount("eur", CurrencyUnit.EUR));
        BalanceAccount rubAccount = treasury.getAccountWithId(new BalanceAccount("rub", Units.RUB));
        BalanceAccount btcAccount = treasury.getAccountWithId(new BalanceAccount("btc", Units.BTC));

        ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);

        // we buy some dollars for a trip abroad, we know how much we've spent
        core.setBuyAmount(1000, 0);
        core.setBuyAmountUnit("USD");
        core.setSellAmount(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN));
        core.setTimestamp(TestUtils.JULY_3RD_2015.inner);
        core.setAgent(agentExchanger);
        core.setBuyAccount(usdAccount);
        core.setSellAccount(rubAccount);
        ratesRepo.addRate(TestUtils.JULY_3RD_2015, Units.RUB,
                CurrencyUnit.USD, CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(55.5))); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);

        Submitter.Result submit = core.submit();
        submit.raiseExceptionIfFailed();

        rubAccount = treasury.getAccountWithId(new BalanceAccount("rub", Units.RUB));
        usdAccount = treasury.getAccountWithId(new BalanceAccount("usd", CurrencyUnit.USD));
        final Optional<CurrencyExchangeEvent> dollarsExc = bundle.currencyExchangeEvents().streamForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue(caseName + "No dollars exchange found", dollarsExc.isPresent());
        assertEquals(
                caseName + "Bad dollars exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 1000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN))
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(56.)).round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.JULY_3RD_2015.inner))
                        .setAgent(agentExchanger)
                        .setBoughtAccount(usdAccount)
                        .setSoldAccount(rubAccount)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(dollarsExc.get())
                        .setRate(dollarsExc.get().rate.round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(dollarsExc.get().timestamp))
                        .build()
        );

        final Optional<FundsMutationEvent> dollarsExcMutation = bundle.fundsMutationEvents().streamForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue(caseName + "No dollars exchange LOSS mutation found", dollarsExcMutation.isPresent());

        assertEquals(
                caseName + "Bad dollars exchange LOSS mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(-9.01)))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.JULY_3RD_2015.inner))
                        .setAgent(agentExchanger)
                        .setRelevantBalance(usdAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(dollarsExcMutation.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(dollarsExcMutation.get().timestamp))
                        .build()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we buy some rubles to spend them home
        core.setSellAmount(1000, 0);
        core.setSellAmountUnit(CurrencyUnit.EUR);
        core.setBuyAmountUnit("RUB");
        core.setTimestamp(TestUtils.DAY_BF_YESTER.inner);
        core.setAgent(agentExchanger);
        core.setCustomRate(BigDecimal.valueOf(64.));
        core.setBuyAccount(rubAccount);
        core.setSellAccount(eurAccount);
        ratesRepo.addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(62.)); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        rubAccount = treasury.getAccountWithId(new BalanceAccount("rub", Units.RUB));
        eurAccount = treasury.getAccountWithId(new BalanceAccount("eur", CurrencyUnit.EUR));
        final Optional<CurrencyExchangeEvent> euroExc = bundle.currencyExchangeEvents().streamForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue(caseName + "No euros exchange found", euroExc.isPresent());
        assertEquals(
                caseName + "Bad euros exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 64000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(CurrencyUnit.EUR, 1000.0, RoundingMode.HALF_DOWN))
                        .setRate(BigDecimal.valueOf(64.))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setAgent(agentExchanger)
                        .setBoughtAccount(rubAccount)
                        .setSoldAccount(eurAccount)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(euroExc.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(euroExc.get().timestamp))
                        .build()
        );
        final Optional<FundsMutationEvent> eurosExcMutation = bundle.fundsMutationEvents().streamForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue(caseName + "No euros exchange BENEFIT mutation found", eurosExcMutation.isPresent());
        assertEquals(
                caseName + "Bad euros exchange BENEFIT mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.RUB, 2000))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setAgent(agentExchanger)
                        .setRelevantBalance(rubAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(eurosExcMutation.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(eurosExcMutation.get().timestamp))
                        .build()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we buy some bitcoins from a dude
        core.setBuyAmountDecimal(BigDecimal.valueOf(1000.));
        core.setBuyAmountUnit(Units.BTC);
        core.setSellAmountUnit("USD");
        core.setTimestamp(TestUtils.DAY_BF_YESTER.inner);
        core.setAgent(agentExchanger);
        core.setCustomRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(260.)));
        core.setBuyAccount(btcAccount);
        core.setSellAccount(usdAccount);
        ratesRepo.addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.USD,
                Units.BTC, CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(250.))); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        btcAccount = treasury.getAccountWithId(new BalanceAccount("btc", Units.BTC));
        usdAccount = treasury.getAccountWithId(new BalanceAccount("usd", CurrencyUnit.USD));
        final Optional<CurrencyExchangeEvent> btcExc = bundle.currencyExchangeEvents().streamForDay(TestUtils.DAY_BF_YESTER)
                .reduce((event, event2) -> event.bought.getCurrencyUnit().equals(Units.BTC) && event.sold.getCurrencyUnit().equals(CurrencyUnit.USD) ? event : event2);
        assertTrue(caseName + "No btc exchange found", btcExc.isPresent());
        assertEquals(
                caseName + "Bad btc exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.BTC, 1000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(CurrencyUnit.USD, 260000.0, RoundingMode.HALF_DOWN))
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(260.)).round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setSoldAccount(usdAccount)
                        .setBoughtAccount(btcAccount)
                        .setAgent(agentExchanger)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(btcExc.get())
                        .setRate(btcExc.get().rate.round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(btcExc.get().timestamp))
                        .build()
        );
        final Optional<FundsMutationEvent> btcExcMutation = bundle.fundsMutationEvents().streamForDay(TestUtils.DAY_BF_YESTER)
                .reduce((event, event2) -> event.amount.getCurrencyUnit().equals(Units.BTC) ? event : event2);
        assertTrue(caseName + "No btc exchange LOSS mutation found", btcExcMutation.isPresent());
        assertEquals(
                caseName + "Bad btc exchange LOSS mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.BTC, -40))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setAgent(agentExchanger)
                        .setRelevantBalance(btcAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(btcExcMutation.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(btcExcMutation.get().timestamp))
                        .build()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we sell some bitcoins at exchange for a fair price
        core.setBuyAmountUnit(CurrencyUnit.USD);
        core.setSellAmountUnit(Units.BTC);
        core.setSellAmountDecimal(BigDecimal.valueOf(1000.));
        core.setTimestamp(TestUtils.YESTERDAY.inner);
        core.setAgent(agentExchanger);
        core.setBuyAccount(usdAccount);
        core.setSellAccount(btcAccount);
        ratesRepo.addRate(TestUtils.YESTERDAY, Units.BTC, CurrencyUnit.USD, BigDecimal.valueOf(265.)); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(false);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<CurrencyExchangeEvent> btcExc2 = bundle.currencyExchangeEvents().streamForDay(TestUtils.YESTERDAY).findFirst();
        assertTrue(caseName + "No btc2 exchange found", btcExc2.isPresent());
        assertEquals(
                caseName + "Bad btc2 exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 265000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(Units.BTC, 1000.0, RoundingMode.HALF_DOWN))
                        .setRate(BigDecimal.valueOf(265.))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.YESTERDAY.inner))
                        .setAgent(agentExchanger)
                        .setBoughtAccount(usdAccount)
                        .setSoldAccount(btcAccount)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(btcExc2.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(btcExc2.get().timestamp))
                        .build()
        );
        final Optional<FundsMutationEvent> btcExcMutation2 = bundle.fundsMutationEvents().streamForDay(TestUtils.YESTERDAY).findFirst();
        assertFalse(caseName + "btc2 exchange LOSS mutation found", btcExcMutation2.isPresent());

        assertEquals(caseName + "Treasury USD register failed", Money.of(CurrencyUnit.USD, 41000), treasury.amount(CurrencyUnit.USD).get());
        assertEquals(caseName + "Treasury RUB register failed", Money.of(Units.RUB, 1008000), treasury.amount(Units.RUB).get());
        assertEquals(caseName + "Treasury BTC register failed", Money.of(Units.BTC, 1100), treasury.amount(Units.BTC).get());
        assertEquals(caseName + "Treasury EUR register failed", Money.zero(CurrencyUnit.EUR), treasury.amount(CurrencyUnit.EUR).get());
    }

}