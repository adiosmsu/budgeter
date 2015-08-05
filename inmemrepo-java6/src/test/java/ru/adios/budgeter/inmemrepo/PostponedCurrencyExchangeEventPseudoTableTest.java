package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;

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
                .rememberPostponedExchange(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), CurrencyUnit.USD, Optional.of(BigDecimal.valueOf(0.89)), OffsetDateTime.now(), agent);
        final int id = Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.idSequence.get();
        assertEquals("Money don't match", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS.get(id).obj.toBuy);
    }

    @Test
    public void testStreamRememberedExchanges() throws Exception {
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .rememberPostponedExchange(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), CurrencyUnit.USD, Optional.of(BigDecimal.valueOf(0.89)),
                        OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), agent);
        Schema.POSTPONED_CURRENCY_EXCHANGE_EVENTS
                .streamRememberedExchanges(new UtcDay(OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.EUR, CurrencyUnit.USD)
                .forEach(new Consumer<PostponedCurrencyExchangeEventRepository.PostponedExchange>() {
                    @Override
                    public void accept(PostponedCurrencyExchangeEventRepository.PostponedExchange postponedExchange) {
                        assertEquals("Wrong stream: " + postponedExchange.toBuy, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), postponedExchange.toBuy);
                    }
                });
    }

}