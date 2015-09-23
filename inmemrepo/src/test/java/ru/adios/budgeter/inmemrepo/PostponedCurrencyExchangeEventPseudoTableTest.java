package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Date: 6/15/15
 * Time: 6:30 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventPseudoTableTest {

    private final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Tesy").build();

    @Before
    public void setUp() {
        Schema.clearSchema();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(agent);
    }

    @Test
    public void testRememberPostponedExchange() throws Exception {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .rememberPostponedExchange(BigDecimal.valueOf(1034530L), TestUtils.prepareBalance(CurrencyUnit.EUR),
                        TestUtils.prepareBalance(CurrencyUnit.USD), Optional.of(BigDecimal.valueOf(0.89)), OffsetDateTime.now(), agent);
        final int id = Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.idSequence.get();
        assertEquals("Money don't match", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), Money.of(CurrencyUnit.EUR, Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.get(id).obj.toBuy));
    }

    @Test
    public void testStreamRememberedExchanges() throws Exception {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .rememberPostponedExchange(BigDecimal.valueOf(1000L), TestUtils.prepareBalance(CurrencyUnit.EUR), TestUtils.prepareBalance(CurrencyUnit.USD), Optional.of(BigDecimal.valueOf(0.89)),
                        OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), agent);
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .streamRememberedExchanges(new UtcDay(OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.EUR, CurrencyUnit.USD)
                .forEach(postponedExchange ->
                        assertEquals("Wrong stream: " + postponedExchange.toBuy, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), Money.of(CurrencyUnit.EUR, postponedExchange.toBuy)));
    }

}