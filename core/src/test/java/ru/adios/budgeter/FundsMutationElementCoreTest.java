package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        final MathContext mc = new MathContext(7, RoundingMode.HALF_DOWN);
        final Accounter accounter = bundle.accounter();
        final Treasury treasury = bundle.treasury();

        final CurrenciesExchangeService ratesService = new CurrenciesExchangeService(
                bundle.getTransactionalSupport(),
                bundle.currencyRates(),
                accounter,
                treasury,
                ExchangeRatesLoader.createBtcLoader(treasury),
                ExchangeRatesLoader.createCbrLoader(treasury)
        );

        bundle.clearSchema();

        Treasury.BalanceAccount rubAccount = TestUtils.prepareBalance(bundle, Units.RUB);
        Treasury.BalanceAccount usdAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        Treasury.BalanceAccount eurAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);

        final FundsMutationAgent groceryAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Магазин").build());
        final FundsMutationAgent inetAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Интернет").build());
        final FundsMutationAgent musicShopAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Music shop").build());
        final FundsMutationSubject breadSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Хлеб")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build()
        );
        final FundsMutationSubject workSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Час работы")
                .setType(FundsMutationSubject.Type.SERVICE)
                .build()
        );
        final FundsMutationSubject cardSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("NVidea 770GTX")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build()
        );
        final FundsMutationSubject guitarSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Gibson Les Paul")
                .setType(FundsMutationSubject.Type.PRODUCT)
                .build()
        );
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

        final Optional<FundsMutationEvent> savedBread = bundle.fundsMutationEvents().streamForDay(TestUtils.TODAY).findFirst();
        assertTrue(caseName + "Bread purchase (not mutated) not found", savedBread.isPresent());
        assertEquals(
                caseName + "Bread purchase (not mutated) fault",
                FundsMutationEvent.builder()
                        .setAgent(groceryAgent)
                        .setAmount(Money.of(Units.RUB, -90.0))
                        .setSubject(breadSubj)
                        .setTimestamp(now)
                        .setRelevantBalance(rubAccount)
                        .build(),
                savedBread.get()
        );
        assertEquals(caseName + "Rubles account fault after bread purchase (not mutated)", Money.zero(Units.RUB), treasury.amountForHumans(Units.RUB));

        bundle.currencyRates().addRate(TestUtils.JULY_3RD_2015, CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
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

        rubAccount = treasury.getAccountForName(rubAccount.name).get();
        final Optional<FundsMutationEvent> savedWorkHour = bundle.fundsMutationEvents().streamForDay(TestUtils.JULY_3RD_2015).filter(event -> event.subject.equals(workSubj)).findFirst();
        assertTrue(caseName + "Work hour deal not found", savedWorkHour.isPresent());
        assertEquals(
                caseName + "Work hour deal fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 6500))
                        .setSubject(workSubj)
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.JULY_3RD_2015.inner))
                        .setRelevantBalance(rubAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(savedWorkHour.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(savedWorkHour.get().timestamp))
                        .build()
        );
        final Optional<FundsMutationEvent> workConversionDelta = bundle.fundsMutationEvents().streamForDay(TestUtils.JULY_3RD_2015)
                .filter(event -> event.subject.equals(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo())))
                .findFirst();
        assertFalse(caseName + "work hour deal: conversion mutation present although custom rate was never used", workConversionDelta.isPresent());
        final Optional<CurrencyExchangeEvent> workHourExchange = bundle.currencyExchangeEvents().streamForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue(caseName + "Work hour exchange not present", workHourExchange.isPresent());
        assertEquals(
                caseName + "Bad work hour exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 6500.0))
                        .setSold(Money.of(CurrencyUnit.EUR, 100.0))
                        .setRate(BigDecimal.valueOf(65.))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.JULY_3RD_2015.inner))
                        .setAgent(inetAgent)
                        .setSoldAccount(Treasury.getTransitoryAccount(CurrencyUnit.EUR, treasury))
                        .setBoughtAccount(rubAccount)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(workHourExchange.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(workHourExchange.get().timestamp))
                        .build()
        );
        assertEquals(caseName + "Rubles account fault after hour work deal", Money.of(Units.RUB, 6500.0), treasury.amountForHumans(Units.RUB));

        bundle.currencyRates().addRate(TestUtils.DAY_BF_YESTER, CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(56));
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

        rubAccount = treasury.getAccountForName(rubAccount.name).get();
        final Optional<FundsMutationEvent> savedCard = bundle.fundsMutationEvents().streamForDay(TestUtils.DAY_BF_YESTER).filter(event -> event.subject.equals(cardSubj)).findFirst();
        assertTrue(caseName + "Card deal not found", savedCard.isPresent());
        assertEquals(
                caseName + "Card deal fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 10000))
                        .setSubject(cardSubj)
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setRelevantBalance(rubAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(savedCard.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(savedCard.get().timestamp))
                        .build()
        );
        final FundsMutationSubject fundsMutSubj = FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo());
        final Optional<FundsMutationEvent> cardDelta = bundle.fundsMutationEvents().streamForDay(TestUtils.DAY_BF_YESTER)
                .filter(event -> event.subject.equals(fundsMutSubj))
                .findFirst();
        assertTrue(caseName + "Card deal: conversion mutation not present although custom rate was used", cardDelta.isPresent());
        assertEquals(
                caseName + "Card deal: conversion mutation fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 344.83))
                        .setSubject(fundsMutSubj)
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setRelevantBalance(rubAccount)
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(cardDelta.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(cardDelta.get().timestamp))
                        .build()
        );
        final Optional<CurrencyExchangeEvent> cardExchange = bundle.currencyExchangeEvents().streamForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue(caseName + "Card exchange not present", cardExchange.isPresent());
        assertEquals(
                caseName + "Bad card exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 10000.0))
                        .setSold(Money.of(CurrencyUnit.USD, 172.41))
                        .setRate(BigDecimal.valueOf(58.))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.DAY_BF_YESTER.inner))
                        .setAgent(inetAgent)
                        .setBoughtAccount(rubAccount)
                        .setSoldAccount(Treasury.getTransitoryAccount(CurrencyUnit.USD, treasury))
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(cardExchange.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(cardExchange.get().timestamp))
                        .build()
        );
        assertEquals(caseName + "Rubles account fault after card deal", Money.of(Units.RUB, 16500.0), treasury.amountForHumans(Units.RUB));

        treasury.addAmount(Money.of(Units.RUB, 500000), "rub");

        bundle.currencyRates().addRate(TestUtils.YESTERDAY, CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
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

        rubAccount = treasury.getAccountForName(rubAccount.name).get();
        final Optional<FundsMutationEvent> savedGuitar = bundle.fundsMutationEvents().streamForDay(TestUtils.YESTERDAY).filter(event -> event.subject.equals(guitarSubj)).findFirst();
        assertTrue(caseName + "Guitar deal not found", savedGuitar.isPresent());
        assertEquals(
                caseName + "Guitar deal fault",
                FundsMutationEvent.builder()
                        .setAgent(musicShopAgent)
                        .setAmount(Money.of(Units.RUB, -280000))
                        .setRelevantBalance(rubAccount)
                        .setSubject(guitarSubj)
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.YESTERDAY.inner))
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(savedGuitar.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(savedGuitar.get().timestamp))
                        .build()
        );
        final Optional<FundsMutationEvent> guitarDelta = bundle.fundsMutationEvents().streamForDay(TestUtils.YESTERDAY)
                .filter(event -> event.subject.equals(fundsMutSubj))
                .findFirst();
        assertTrue(caseName + "Guitar deal: conversion mutation not present although custom rate was used", guitarDelta.isPresent());
        assertEquals(
                caseName + "Guitar deal: conversion mutation fault",
                FundsMutationEvent.builder()
                        .setAgent(musicShopAgent)
                        .setAmount(Money.of(Units.RUB, -5000))
                        .setRelevantBalance(rubAccount)
                        .setSubject(fundsMutSubj)
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.YESTERDAY.inner))
                        .build(),
                FundsMutationEvent.builder()
                        .setFundsMutationEvent(guitarDelta.get())
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(guitarDelta.get().timestamp))
                        .build()
        );
        final Optional<CurrencyExchangeEvent> guitarExchange = bundle.currencyExchangeEvents().streamForDay(TestUtils.YESTERDAY).findFirst();
        assertTrue(caseName + "Guitar exchange not present", guitarExchange.isPresent());
        assertEquals(
                caseName + "Bad guitar exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 5000.0))
                        .setSold(Money.of(Units.RUB, 280000.0))
                        .setBoughtAccount(Treasury.getTransitoryAccount(CurrencyUnit.USD, treasury))
                        .setSoldAccount(rubAccount)
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(56.)).round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(TestUtils.YESTERDAY.inner))
                        .setAgent(musicShopAgent)
                        .build(),
                CurrencyExchangeEvent.builder()
                        .setEvent(guitarExchange.get())
                        .setRate(guitarExchange.get().rate.round(mc))
                        .setTimestamp(DateTimeUtils.convertToCurrentZone(guitarExchange.get().timestamp))
                        .build()
        );
        assertEquals(caseName + "Rubles account fault after guitar deal", Money.of(Units.RUB, 236500.0), treasury.amountForHumans(Units.RUB));
    }
}