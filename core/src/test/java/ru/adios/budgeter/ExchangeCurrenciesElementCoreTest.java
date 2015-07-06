package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        ExchangeCurrenciesElementCore core = new ExchangeCurrenciesElementCore(accounter, treasury, ratesService);

        // we buy some dollars for a trip abroad, we know how much we've spent
        core.setBuyAmount(1000, 0);
        core.setBuyAmountUnit("USD");
        core.setSellAmount(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN));
        core.setTimestamp(TestUtils.JULY_3RD_2015.inner);
        core.setAgent(agentExchanger);
        ratesRepo.addRate(TestUtils.JULY_3RD_2015, Units.RUB, CurrencyUnit.USD, BigDecimal.valueOf(55.5)); // to know exact value we set "natural" rate ourselves
        core.submit();
        final Optional<CurrencyExchangeEvent> dollarsExc = accounter.streamExchangesForDay(TestUtils.JULY_3RD_2015).findFirst();
        assertTrue("No dollars exchange found", dollarsExc.isPresent());
        assertEquals(
                "Bad dollars exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(CurrencyUnit.USD, 1000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(Units.RUB, 56000.0, RoundingMode.HALF_DOWN))
                        .setRate(BigDecimal.valueOf(56.))
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
                        .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(500L)))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifference(accounter.fundsMutationSubjectRepo()))
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
        core.setCustomRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(64.)));
        ratesRepo.addRate(TestUtils.DAY_BF_YESTER, Units.RUB, CurrencyUnit.EUR, BigDecimal.valueOf(62.)); // to know exact value we set "natural" rate ourselves
        core.submit();
        final Optional<CurrencyExchangeEvent> euroExc = accounter.streamExchangesForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue("No euros exchange found", euroExc.isPresent());
        assertEquals(
                "Bad euros exchange values",
                CurrencyExchangeEvent.builder()
                        .setBought(Money.of(Units.RUB, 64000.0, RoundingMode.HALF_DOWN))
                        .setSold(Money.of(CurrencyUnit.EUR, 1000.0, RoundingMode.HALF_DOWN))
                        .setRate(CurrencyRatesProvider.reverseRate(BigDecimal.valueOf(64.)))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                euroExc.get()
        );
        final Optional<FundsMutationEvent> eurosExcMutation = accounter.streamMutationsForDay(TestUtils.DAY_BF_YESTER).findFirst();
        assertTrue("No euros exchange LOSS mutation found", eurosExcMutation.isPresent());
        assertEquals(
                "Bad euros exchange LOSS mutation values",
                FundsMutationEvent.builder()
                        .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(500L)))
                        .setQuantity(1)
                        .setSubject(FundsMutationSubject.getCurrencyConversionDifference(accounter.fundsMutationSubjectRepo()))
                        .setTimestamp(TestUtils.DAY_BF_YESTER.inner)
                        .setAgent(agentExchanger)
                        .build(),
                eurosExcMutation.get()
        );
    }

}