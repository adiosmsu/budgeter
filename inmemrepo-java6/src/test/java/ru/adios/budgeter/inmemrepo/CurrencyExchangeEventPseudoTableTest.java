package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.Units;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Date: 6/15/15
 * Time: 1:13 PM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventPseudoTableTest {

    @Test
    public void testRegisterCurrencyExchange() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.now();
        Schema.FUNDS_MUTATION_AGENTS.clear();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(agent);
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(30000L)))
                .setSold(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(1000L)))
                .setRate(BigDecimal.valueOf(30L))
                .setTimestamp(ts)
                .setAgent(agent)
                .build();
        Schema.CURRENCY_EXCHANGE_EVENTS.registerCurrencyExchange(exchangeEvent);
        assertEquals("Storage broke on put/get test", exchangeEvent, Schema.CURRENCY_EXCHANGE_EVENTS.get(Schema.CURRENCY_EXCHANGE_EVENTS.idSequence.get()).obj);
    }

}