/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;
import ru.adios.budgeter.api.data.FundsMutationAgent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Date: 10/26/15
 * Time: 2:49 PM
 *
 * @author Mikhail Kulikov
 */
public final class CurrencyExchangeTester {

    private final Bundle bundle;

    public CurrencyExchangeTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setUp() {
        bundle.clearSchema();
    }

    public void testRegisterCurrencyExchange() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.now();
        bundle.clear(Bundle.Repo.FUNDS_MUTATION_AGENTS);
        final FundsMutationAgent agent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Test").build());
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(30000L)))
                .setSold(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(TestUtils.prepareBalance(bundle, Units.RUB))
                .setSoldAccount(TestUtils.prepareBalance(bundle, CurrencyUnit.USD))
                .setRate(BigDecimal.valueOf(30L))
                .setTimestamp(ts)
                .setAgent(agent)
                .build();
        final CurrencyExchangeEventRepository exEventsRepo = bundle.currencyExchangeEvents();
        exEventsRepo.registerCurrencyExchange(exchangeEvent);
        assertEquals("Storage broke on put/get test", exchangeEvent, exEventsRepo.getById(exEventsRepo.currentSeqValue()).get());
    }

    public void testCount() throws Exception {
        final CurrencyExchangeEventRepository curExEvents = bundle.currencyExchangeEvents();
        testStreamExchangeEvents();

        assertEquals("Wrong number of records", 2, curExEvents.countExchangeEvents());

        final FundsMutationAgent agent = bundle.fundsMutationAgents().findByName("Test").get();
        final CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(40000L)))
                .setSold(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(TestUtils.prepareBalance(bundle, Units.RUB))
                .setSoldAccount(TestUtils.prepareBalance(bundle, CurrencyUnit.USD))
                .setRate(BigDecimal.valueOf(40L))
                .setTimestamp(new UtcDay().add(-2).inner)
                .setAgent(agent)
                .build();
        curExEvents.registerCurrencyExchange(exchangeEvent);

        assertEquals("Wrong number of records", 3, curExEvents.countExchangeEvents());
    }

    public void testStreamExchangeEvents() throws Exception {
        testRegisterCurrencyExchange();
        final CurrencyExchangeEventRepository curExEvents = bundle.currencyExchangeEvents();
        final CurrencyExchangeEvent currencyExchangeEvent1 = curExEvents.getById(curExEvents.currentSeqValue()).get();

        final FundsMutationAgent agent = bundle.fundsMutationAgents().getAgentWithId(FundsMutationAgent.builder().setName("Test").build());
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(70000L)))
                .setSold(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(bundle.treasury().getAccountForName("accountRUB").get())
                .setSoldAccount(TestUtils.prepareBalance(bundle, CurrencyUnit.EUR))
                .setRate(BigDecimal.valueOf(70L))
                .setTimestamp(OffsetDateTime.now().plus(1, ChronoUnit.MINUTES))
                .setAgent(agent)
                .build();
        curExEvents.registerCurrencyExchange(exchangeEvent);

        assertEquals(1, curExEvents.streamExchangeEvents(OptLimit.createLimit(1)).count());
        assertEquals(1, curExEvents.streamExchangeEvents(OptLimit.createOffset(1)).count());
        assertEquals(1, curExEvents.streamExchangeEvents(OptLimit.create(1, 1)).count());
        assertEquals(0, curExEvents.streamExchangeEvents(OptLimit.createOffset(2)).count());
        assertTrue(curExEvents
                        .streamExchangeEvents(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.ASC))
                        .findFirst()
                        .get()
                        .bought.isEqual(Money.of(Units.RUB, BigDecimal.valueOf(30000L)))
        );
        final CurrencyExchangeEvent currencyExchangeEvent = curExEvents
                .streamExchangeEvents(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC))
                .findFirst()
                .get();
        final Money bought = currencyExchangeEvent
                .bought;
        System.out.println("---------------->" + bought);
        assertTrue("Not 70000: " + bought + ", time=" + currencyExchangeEvent.timestamp + ", old time=" + currencyExchangeEvent1.timestamp,
                bought.isEqual(Money.of(Units.RUB, BigDecimal.valueOf(70000L))));
    }

    public void testStreamForDay() throws Exception {
        testStreamExchangeEvents();

        final FundsMutationAgent agent = bundle.fundsMutationAgents().getAgentWithId(FundsMutationAgent.builder().setName("Test").build());
        CurrencyExchangeEvent exchangeEvent = CurrencyExchangeEvent.builder()
                .setBought(Money.of(Units.RUB, BigDecimal.valueOf(70000L)))
                .setSold(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)))
                .setBoughtAccount(bundle.treasury().getAccountForName("accountRUB").get())
                .setSoldAccount(bundle.treasury().getAccountForName("accountEUR").get())
                .setRate(BigDecimal.valueOf(70L))
                .setTimestamp(new UtcDay().add(-10).inner)
                .setAgent(agent)
                .build();


        assertEquals("Stream for day counted wrong", 2, bundle.currencyExchangeEvents().streamForDay(new UtcDay()).count());
    }

}
