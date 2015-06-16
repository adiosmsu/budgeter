package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.CurrencyExchangeEvent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        CurrencyUnit rub = CurrencyUnit.of("RUB");
        final OffsetDateTime ts = OffsetDateTime.now();
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(rub, BigDecimal.valueOf(30000L)))
                .setSold(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(1000L)))
                .setRate(BigDecimal.valueOf(30L))
                .setTimestamp(ts)
                .build();
        Schema.CURRENCY_EXCHANGE_EVENTS.registerCurrencyExchange(exchangeEvent);
        assertEquals("Storage broke on put/get test", exchangeEvent, Schema.CURRENCY_EXCHANGE_EVENTS.get(Schema.CURRENCY_EXCHANGE_EVENTS.idSequence.get()).obj);
    }

}