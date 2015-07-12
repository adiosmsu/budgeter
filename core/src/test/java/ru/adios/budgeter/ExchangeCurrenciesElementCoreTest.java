package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
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
        final AccounterMock accounter = new AccounterMock();
        final TreasuryMock treasury = new TreasuryMock();
        final TransactionalSupportMock txNal = new TransactionalSupportMock();
        final CurrencyRatesRepositoryMock ratesRepo = new CurrencyRatesRepositoryMock();
        final CurrenciesExchangeService ratesService =
                new CurrenciesExchangeService(txNal, ratesRepo, accounter, treasury, ExchangeRatesLoader.createBtcLoader(treasury), ExchangeRatesLoader.createCbrLoader(treasury));

        Schema.clearSchema();

        final FundsMutationAgent agentExchanger = FundsMutationAgent.builder().setName("Обменник").build();
        accounter.fundsMutationAgentRepo().addAgent(agentExchanger);

        treasury.addAmount(Money.of(CurrencyUnit.USD, 300000));
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 1000));
        treasury.addAmount(Money.of(Units.RUB, 1000000));
        treasury.addAmount(Money.of(Units.BTC, 100));

        ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);

        // we buy some dollars for a trip abroad, we know how much we've spent
        core.setBuyAmount(1000, 0);
        core.setBuyAmountUnit("USD");
        core.setSellAmount(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN));
        core.setTimestamp(TestUtils.JULY_3RD_2015.inner);
        core.setAgent(agentExchanger);
        ratesRepo.addRate(TestUtils.JULY_3RD_2015, Units.RUB, CurrencyUnit.USD, CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(55.5))); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);
        core.submit();
        final Optional<CurrencyExchangeEvent> dollarsExc = accounter.streamExchangesForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue("No dollars exchange found", dollarsExc.isPresent());
        assertEquals(
                "Bad dollars exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 1000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN))
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(56.)))
                        .setTimestamp(TestUtils.JULY_3RD_2015.inner)
                        .setAgent(agentExchanger)
                        .build(),
                dollarsExc.get()
        );
        final Optional<FundsMutationEvent> dollarsExcMutation = accounter.streamMutationsForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue("No dollars exchange LOSS mutation found", dollarsExcMutation.isPresent());
        assertEquals(
                "Bad dollars exchange LOSS mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(-9.01)))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(TestUtils.JULY_3RD_2015.inner)
                        .setAgent(agentExchanger)
                        .build(),
                dollarsExcMutation.get()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we buy some rubles to spend them home
        core.setSellAmount(1000, 0);
        core.setSellAmountUnit(CurrencyUnit.EUR);
        core.setBuyAmountUnit("RUB");
        core.setTimestamp(TestUtils.DAY_BF_YESTER.inner);
        core.setAgent(agentExchanger);
        core.setCustomRate(BigDecimal.valueOf(64.));
        ratesRepo.addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(62.)); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);
        core.submit();
        final Optional<CurrencyExchangeEvent> euroExc = accounter.streamExchangesForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue("No euros exchange found", euroExc.isPresent());
        assertEquals(
                "Bad euros exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 64000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(CurrencyUnit.EUR, 1000.0, RoundingMode.HALF_DOWN))
                        .setRate(BigDecimal.valueOf(64.))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                euroExc.get()
        );
        final Optional<FundsMutationEvent> eurosExcMutation = accounter.streamMutationsForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue("No euros exchange BENEFIT mutation found", eurosExcMutation.isPresent());
        assertEquals(
                "Bad euros exchange BENEFIT mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.RUB, 2000))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                eurosExcMutation.get()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we buy some bitcoins from a dude
        core.setBuyAmountDecimal(BigDecimal.valueOf(1000.));
        core.setBuyAmountUnit(Units.BTC);
        core.setSellAmountUnit("USD");
        core.setTimestamp(TestUtils.DAY_BF_YESTER.inner);
        core.setAgent(agentExchanger);
        core.setCustomRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(260.)));
        ratesRepo.addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.USD, Units.BTC, CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(250.))); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(true);
        core.submit();
        final Optional<CurrencyExchangeEvent> btcExc = accounter.streamExchangesForDay(TestUtils.DAY_BF_YESTER)
                .reduce((event, event2) -> event.bought.getCurrencyUnit().equals(Units.BTC) && event.sold.getCurrencyUnit().equals(CurrencyUnit.USD) ? event : event2);
        assertTrue("No btc exchange found", btcExc.isPresent());
        assertEquals(
                "Bad btc exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.BTC, 1000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(CurrencyUnit.USD, 260000.0, RoundingMode.HALF_DOWN))
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(260.)))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                btcExc.get()
        );
        final Optional<FundsMutationEvent> btcExcMutation = accounter.streamMutationsForDay(TestUtils.DAY_BF_YESTER)
                .reduce((event, event2) -> event.amount.getCurrencyUnit().equals(Units.BTC) ? event : event2);
        assertTrue("No btc exchange LOSS mutation found", btcExcMutation.isPresent());
        assertEquals(
                "Bad btc exchange LOSS mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.BTC, -40))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                btcExcMutation.get()
        );

        core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);
        // we sell some bitcoins at exchange for a fair price
        core.setBuyAmountUnit(CurrencyUnit.USD);
        core.setSellAmountUnit(Units.BTC);
        core.setSellAmountDecimal(BigDecimal.valueOf(1000.));
        core.setTimestamp(TestUtils.YESTERDAY.inner);
        core.setAgent(agentExchanger);
        ratesRepo.addRate(TestUtils.YESTERDAY, Units.BTC, CurrencyUnit.USD, BigDecimal.valueOf(265.)); // to know exact value we set "natural" rate ourselves
        core.setPersonalMoneyExchange(false);
        core.submit();
        final Optional<CurrencyExchangeEvent> btcExc2 = accounter.streamExchangesForDay(TestUtils.YESTERDAY).findFirst();
        assertTrue("No btc2 exchange found", btcExc2.isPresent());
        assertEquals(
                "Bad btc2 exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 265000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(Units.BTC, 1000.0, RoundingMode.HALF_DOWN))
                        .setRate(BigDecimal.valueOf(265.))
                        .setTimestamp(TestUtils.YESTERDAY.inner)
                        .setAgent(agentExchanger)
                        .build(),
                btcExc2.get()
        );
        final Optional<FundsMutationEvent> btcExcMutation2 = accounter.streamMutationsForDay(TestUtils.YESTERDAY).findFirst();
        assertFalse("btc2 exchange LOSS mutation found", btcExcMutation2.isPresent());

        assertEquals("Treasury USD register failed", Money.of(CurrencyUnit.USD, 41000), treasury.amount(CurrencyUnit.USD).get());
        assertEquals("Treasury RUB register failed", Money.of(Units.RUB, 1008000), treasury.amount(Units.RUB).get());
        assertEquals("Treasury BTC register failed", Money.of(Units.BTC, 1100), treasury.amount(Units.BTC).get());
        assertEquals("Treasury EUR register failed", Money.zero(CurrencyUnit.EUR), treasury.amount(CurrencyUnit.EUR).get());
    }

}