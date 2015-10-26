package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Date: 7/9/15
 * Time: 7:15 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        final AccounterMock accounter = new AccounterMock();
        final TreasuryMock treasury = new TreasuryMock();

        final CurrenciesExchangeService ratesService = new CurrenciesExchangeService(
                new TransactionalSupportMock(),
                new CurrencyRatesRepositoryMock(),
                accounter,
                treasury,
                ExchangeRatesLoader.createBtcLoader(treasury),
                ExchangeRatesLoader.createCbrLoader(treasury)
        );

        Schema.clearSchemaStatic();

        final Treasury.BalanceAccount rubAccount = TestUtils.prepareBalance(Units.RUB);
        final Treasury.BalanceAccount usdAccount = TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount eurAccount = TestUtils.prepareBalance(CurrencyUnit.EUR);

        final FundsMutationAgent groceryAgent = FundsMutationAgent.builder().setName("Магазин").build();
        final FundsMutationAgent musicShopAgent = FundsMutationAgent.builder().setName("Music shop").build();
        final FundsMutationAgent inetAgent = FundsMutationAgent.builder().setName("Интернет").build();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(groceryAgent);
        Schema.FUNDS_MUTATION_AGENTS.addAgent(inetAgent);
        Schema.FUNDS_MUTATION_AGENTS.addAgent(musicShopAgent);
        final FundsMutationSubject breadSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Хлеб")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build();
        final FundsMutationSubject workSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Час работы")
                .setType(FundsMutationSubject.Type.SERVICE)
                .build();
        final FundsMutationSubject cardSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("NVidea 770GTX")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build();
        final FundsMutationSubject guitarSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Gibson Les Paul")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(breadSubj);
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(workSubj);
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(cardSubj);
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(guitarSubj);
        final OffsetDateTime now = OffsetDateTime.now();

        // let's buy some bread at a local grocery
        FundsMutationElementCore core = new FundsMutationElementCore(accounter, treasury, ratesService);
        core.setAgent(groceryAgent);
        core.setDirection(FundsMutator.MutationDirection.LOSS);
        core.setSubject(breadSubj);
        core.setAmountUnit(Units.RUB);
        core.setAmount(90, 0);
        core.setMutateFunds(false);
        core.setTimestamp(now);
        core.setRelevantBalance(rubAccount);

        Submitter.Result submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<FundsMutationEvent> savedBread = accounter.streamMutationsForDay(TestUtils.TODAY).findFirst();
        assertTrue("Bread purchase (not mutated) not found", savedBread.isPresent());
        assertEquals(
                "Bread purchase (not mutated) fault",
                FundsMutationEvent.builder()
                        .setAgent(groceryAgent)
                        .setAmount(Money.of(Units.RUB, -90.0))
                        .setSubject(breadSubj)
                        .setTimestamp(now)
                        .setRelevantBalance(rubAccount)
                        .build(),
                savedBread.get()
        );
        assertEquals("Rubles account fault after bread purchase (not mutated)", Money.zero(Units.RUB), treasury.amountForHumans(Units.RUB));

        Schema.CURRENCY_RATES.addRate(TestUtils.JULY_3RD_2015, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
        // let's sell hour of our time for euros with immediate conversion
        core = new FundsMutationElementCore(accounter, treasury, ratesService);
        core.setAgent(inetAgent);
        core.setDirection(FundsMutator.MutationDirection.BENEFIT);
        core.setSubject(workSubj);
        core.setAmountUnit(Units.RUB);
        core.setPaidMoney(Money.of(CurrencyUnit.EUR, 100));
        core.setTimestamp(TestUtils.JULY_3RD_2015.inner);
        core.setRelevantBalance(rubAccount);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<FundsMutationEvent> savedWorkHour = accounter.streamMutationsForDay(TestUtils.JULY_3RD_2015).filter(event -> event.subject.equals(workSubj)).findFirst();
        assertTrue("Work hour deal not found", savedWorkHour.isPresent());
        assertEquals(
                "Work hour deal fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 6500))
                        .setSubject(workSubj)
                        .setTimestamp(TestUtils.JULY_3RD_2015.inner)
                        .setRelevantBalance(rubAccount)
                        .build(),
                savedWorkHour.get()
        );
        final Optional<FundsMutationEvent> workConversionDelta = accounter.streamMutationsForDay(TestUtils.JULY_3RD_2015)
                .filter(event -> event.subject.equals(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo())))
                .findFirst();
        assertFalse("work hour deal: conversion mutation present although custom rate was never used", workConversionDelta.isPresent());
        final Optional<CurrencyExchangeEvent> workHourExchange = accounter.streamExchangesForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue("Work hour exchange not present", workHourExchange.isPresent());
        assertEquals(
                "Bad work hour exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 6500.0))
                        .setSold(Money.of(CurrencyUnit.EUR, 100.0))
                        .setRate(BigDecimal.valueOf(65.))
                        .setTimestamp(TestUtils.JULY_3RD_2015.inner)
                        .setAgent(inetAgent)
                        .setSoldAccount(Treasury.getTransitoryAccount(CurrencyUnit.EUR, treasury))
                        .setBoughtAccount(rubAccount)
                        .build(),
                workHourExchange.get()
        );
        assertEquals("Rubles account fault after hour work deal", Money.of(Units.RUB, 6500.0), treasury.amountForHumans(Units.RUB));

        Schema.CURRENCY_RATES.addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(56));
        // let's sell our video card to a foreign dude paying in dollars with immediate conversion to rubles on a custom rate
        core = new FundsMutationElementCore(accounter, treasury, ratesService);
        core.setAgentString("Интернет");
        core.setDirection(FundsMutator.MutationDirection.BENEFIT);
        core.setSubject(cardSubj);
        core.setAmountUnit("RUB");
        core.setAmountDecimal(BigDecimal.valueOf(10000));
        core.setPayeeAccountUnit(CurrencyUnit.USD);
        core.setCustomRate(BigDecimal.valueOf(58));
        core.setTimestamp(TestUtils.DAY_BF_YESTER.inner);
        core.setRelevantBalance(rubAccount);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<FundsMutationEvent> savedCard = accounter.streamMutationsForDay(TestUtils.DAY_BF_YESTER).filter(event -> event.subject.equals(cardSubj)).findFirst();
        assertTrue("Card deal not found", savedCard.isPresent());
        assertEquals(
                "Card deal fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 10000))
                        .setSubject(cardSubj)
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setRelevantBalance(rubAccount)
                        .build(),
                savedCard.get()
        );
        final FundsMutationSubject fundsMutSubj = FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo());
        final Optional<FundsMutationEvent> cardDelta = accounter.streamMutationsForDay(TestUtils.DAY_BF_YESTER)
                .filter(event -> event.subject.equals(fundsMutSubj))
                .findFirst();
        assertTrue("Card deal: conversion mutation not present although custom rate was used", cardDelta.isPresent());
        assertEquals(
                "Card deal: conversion mutation fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 344.83))
                        .setSubject(fundsMutSubj)
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setRelevantBalance(rubAccount)
                        .build(),
                cardDelta.get()
        );
        final Optional<CurrencyExchangeEvent> cardExchange = accounter.streamExchangesForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue("Card exchange not present", cardExchange.isPresent());
        assertEquals(
                "Bad card exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 10000.0))
                        .setSold(Money.of(CurrencyUnit.USD, 172.41))
                        .setRate(BigDecimal.valueOf(58.))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(inetAgent)
                        .setBoughtAccount(rubAccount)
                        .setSoldAccount(Treasury.getTransitoryAccount(CurrencyUnit.USD, treasury))
                        .build(),
                cardExchange.get()
        );
        assertEquals("Rubles account fault after card deal", Money.of(Units.RUB, 16500.0), treasury.amountForHumans(Units.RUB));

        treasury.addAmount(Money.of(Units.RUB, 500000), "rub");

        Schema.CURRENCY_RATES.addRate(TestUtils.YESTERDAY, CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
        // let's buy the guitar abroad using RUB debit card
        core = new FundsMutationElementCore(accounter, treasury, ratesService);
        core.setAgent(musicShopAgent);
        core.setDirection(FundsMutator.MutationDirection.LOSS);
        core.setSubject("Gibson Les Paul");
        core.setAmount(Money.of(CurrencyUnit.USD, 5000));
        core.setPayeeAccountUnit("RUB");
        core.setPayeeAmount(BigDecimal.valueOf(280000));
        core.setPayeeAmount(280000, 0);
        core.setRelevantBalance(rubAccount);
        core.setTimestamp(TestUtils.YESTERDAY.inner);

        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<FundsMutationEvent> savedGuitar = accounter.streamMutationsForDay(TestUtils.YESTERDAY).filter(event -> event.subject.equals(guitarSubj)).findFirst();
        assertTrue("Guitar deal not found", savedGuitar.isPresent());
        assertEquals(
                "Guitar deal fault",
                FundsMutationEvent.builder()
                        .setAgent(musicShopAgent)
                        .setAmount(Money.of(Units.RUB, -280000))
                        .setRelevantBalance(rubAccount)
                        .setSubject(guitarSubj)
                        .setTimestamp(TestUtils.YESTERDAY.inner)
                        .build(),
                savedGuitar.get()
        );
        final Optional<FundsMutationEvent> guitarDelta = accounter.streamMutationsForDay(TestUtils.YESTERDAY)
                .filter(event -> event.subject.equals(fundsMutSubj))
                .findFirst();
        assertTrue("Guitar deal: conversion mutation not present although custom rate was used", guitarDelta.isPresent());
        assertEquals(
                "Guitar deal: conversion mutation fault",
                FundsMutationEvent.builder()
                        .setAgent(musicShopAgent)
                        .setAmount(Money.of(Units.RUB, -5000))
                        .setRelevantBalance(rubAccount)
                        .setSubject(fundsMutSubj)
                        .setTimestamp(TestUtils.YESTERDAY.inner)
                        .build(),
                guitarDelta.get()
        );
        final Optional<CurrencyExchangeEvent> guitarExchange = accounter.streamExchangesForDay(TestUtils.YESTERDAY).findFirst();
        assertTrue("Guitar exchange not present", guitarExchange.isPresent());
        assertEquals(
                "Bad guitar exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 5000.0))
                        .setSold(Money.of(Units.RUB, 280000.0))
                        .setBoughtAccount(Treasury.getTransitoryAccount(CurrencyUnit.USD, treasury))
                        .setSoldAccount(rubAccount)
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(56.)))
                        .setTimestamp(TestUtils.YESTERDAY.inner)
                        .setAgent(musicShopAgent)
                        .build(),
                guitarExchange.get()
        );
        assertEquals("Rubles account fault after guitar deal", Money.of(Units.RUB, 236500.0), treasury.amountForHumans(Units.RUB));
    }

}