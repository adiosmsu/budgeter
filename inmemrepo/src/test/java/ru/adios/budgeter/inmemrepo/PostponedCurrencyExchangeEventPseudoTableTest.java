package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
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

    @Test
    public void testRememberPostponedExchange() throws Exception {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .rememberPostponedExchange(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), CurrencyUnit.USD, Optional.of(BigDecimal.valueOf(0.89)), OffsetDateTime.now());
        final int id = Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.idSequence.get();
        assertEquals("Money don't match", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.get(id).obj.toBuy);
    }

    @Test
    public void testStreamRememberedExchanges() throws Exception {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .rememberPostponedExchange(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), CurrencyUnit.USD, Optional.of(BigDecimal.valueOf(0.89)),
                        OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .streamRememberedExchanges(new UtcDay(OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.EUR, CurrencyUnit.USD)
                .forEach(postponedExchange -> assertEquals("Wrong stream: " + postponedExchange.toBuy, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), postponedExchange.toBuy));
    }

}