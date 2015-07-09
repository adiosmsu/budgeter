package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        Schema.clearSchema();

        final FundsMutationAgent groceryAgent = FundsMutationAgent.builder().setName("Магазин").build();
        final FundsMutationAgent inetAgent = FundsMutationAgent.builder().setName("Интернет").build();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(groceryAgent);
        Schema.FUNDS_MUTATION_AGENTS.addAgent(inetAgent);
        final FundsMutationSubject breadSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Хлеб")
                .setType(FundsMutationSubject.SubjectType.PRODUCT)
                .build();
        final FundsMutationSubject workSubj = FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                .setName("Час работы")
                .setType(FundsMutationSubject.SubjectType.SERVICE)
                .build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(breadSubj);
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(workSubj);
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
        core.submit();
        final Optional<FundsMutationEvent> savedBread = accounter.streamMutationsForDay(TestUtils.TODAY).findFirst();
        assertTrue("Bread purchase (not mutated) not found", savedBread.isPresent());
        assertEquals(
                "Bread purchase (not mutated) fault",
                FundsMutationEvent.builder()
                        .setAgent(groceryAgent)
                        .setAmount(Money.of(Units.RUB, -90.0))
                        .setSubject(breadSubj)
                        .setTimestamp(now)
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
        core.setPayedMoney(Money.of(CurrencyUnit.EUR, 100));
        core.setTimestamp(TestUtils.JULY_3RD_2015.inner);
        core.submit();
        final Optional<FundsMutationEvent> savedWorkHour = accounter.streamMutationsForDay(TestUtils.JULY_3RD_2015).filter(event -> event.subject.equals(workSubj)).findFirst();
        assertTrue("Work hour deal not found", savedWorkHour.isPresent());
        assertEquals(
                "Work hour deal fault",
                FundsMutationEvent.builder()
                        .setAgent(inetAgent)
                        .setAmount(Money.of(Units.RUB, 6500))
                        .setSubject(workSubj)
                        .setTimestamp(TestUtils.JULY_3RD_2015.inner)
                        .build(),
                savedWorkHour.get()
        );

//        final Optional<FundsMutationEvent> workHourConversionDelta = accounter.streamMutationsForDay(TestUtils.JULY_3RD_2015)
//                .filter(event -> event.subject.equals(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo())))
//                .findFirst();
    }

}