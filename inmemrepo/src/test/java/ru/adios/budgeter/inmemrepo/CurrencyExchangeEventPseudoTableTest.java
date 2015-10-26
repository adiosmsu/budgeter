package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Date: 6/15/15
 * Time: 1:13 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventPseudoTableTest {

    @Before
    public void setUp() {
        Schema.clearSchemaStatic();
    }

    @Test
    public void testRegisterCurrencyExchange() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.now();
        Schema.FUNDS_MUTATION_AGENTS.clear();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(agent);
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(30000L)))
                .setSold(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(TestUtils.prepareBalance(Units.RUB))
                .setSoldAccount(TestUtils.prepareBalance(CurrencyUnit.USD))
                .setRate(BigDecimal.valueOf(30L))
                .setTimestamp(ts)
                .setAgent(agent)
                .build();
        Schema.CURRENCY_EXCHANGE_EVENTS.registerCurrencyExchange(exchangeEvent);
        assertEquals("Storage broke on put/get test", exchangeEvent, Schema.CURRENCY_EXCHANGE_EVENTS.get(Schema.CURRENCY_EXCHANGE_EVENTS.idSequence.get()).obj);
    }

    @Test
    public void testStreamExchangeEvents() throws Exception {
        testRegisterCurrencyExchange();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(70000L)))
                .setSold(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(Schema.TREASURY.getAccountForName("accountRUB").get())
                .setSoldAccount(TestUtils.prepareBalance(CurrencyUnit.EUR))
                .setRate(BigDecimal.valueOf(70L))
                .setTimestamp(OffsetDateTime.now())
                .setAgent(agent)
                .build();
        Schema.CURRENCY_EXCHANGE_EVENTS.registerCurrencyExchange(exchangeEvent);

        assertEquals(1, Schema.CURRENCY_EXCHANGE_EVENTS.streamExchangeEvents(OptLimit.createLimit(1)).count());
        assertEquals(1, Schema.CURRENCY_EXCHANGE_EVENTS.streamExchangeEvents(OptLimit.createOffset(1)).count());
        assertEquals(1, Schema.CURRENCY_EXCHANGE_EVENTS.streamExchangeEvents(OptLimit.create(1, 1)).count());
        assertEquals(0, Schema.CURRENCY_EXCHANGE_EVENTS.streamExchangeEvents(OptLimit.createOffset(2)).count());
        assertTrue(Schema.CURRENCY_EXCHANGE_EVENTS
                        .streamExchangeEvents(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.ASC))
                        .findFirst()
                        .get()
                        .bought.isEqual(Money.of(Units.RUB, BigDecimal.valueOf(30000L)))
        );
        assertTrue(Schema.CURRENCY_EXCHANGE_EVENTS
                        .streamExchangeEvents(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC))
                        .findFirst()
                        .get()
                        .bought.isEqual(Money.of(Units.RUB, BigDecimal.valueOf(70000L)))
        );

    }

}
