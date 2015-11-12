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

import org.joda.money.Money;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Date: 11/9/15
 * Time: 10:33 AM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPriceRepoTester {

    private final Bundle bundle;

    public SubjectPriceRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setUp() {
        bundle.clearSchema();
    }

    public void testRegister() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final UtcDay now = new UtcDay();

        final SubjectPrice breadPrice = registerSome(subjectPrices, food, agent, now);
        assertEquals("No breadPrice found", breadPrice, subjectPrices.getById(subjectPrices.currentSeqValue()).get());

        try {
            final SubjectPrice test = SubjectPrice.builder()
                    .setPrice(Money.of(Units.RUB, BigDecimal.valueOf(-50L)))
                    .setSubject(food)
                    .setAgent(agent)
                    .build();
            fail("Subject price instantiated with negative value");
        } catch (Exception ignore) {}

        try {
            final SubjectPrice test = SubjectPrice.builder()
                    .setPrice(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setDay(now)
                    .setSubject(food)
                    .setAgent(agent)
                    .build();

            subjectPrices.register(test);
            fail("Subject price unique constraint violated by repository object");
        } catch (Exception ignore) {}
    }

    public void testPriceExists() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final UtcDay now = new UtcDay();

        registerSome(subjectPrices, food, agent, now);

        assertTrue("Price does not exist though registered", subjectPrices.priceExists(food, agent, now));
    }

    public void testCountByAgent() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        registerSome(subjectPrices, food, agent, new UtcDay());
        registerSome(subjectPrices, food, agent, new UtcDay().add(1));

        assertEquals("Wrong number of prices", 2, subjectPrices.countByAgent(food, agent));
        assertEquals("Wrong number of prices (String counter edition)", 2, subjectPrices.countByAgent(food.name, agent.name));
    }

    public void testStreamByAgent() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        registerSome(subjectPrices, food, agent, new UtcDay());
        final SubjectPrice latest = registerSome(subjectPrices, food, agent, new UtcDay().add(1));
        final SubjectPrice farthest = registerSome(subjectPrices, food, agent, new UtcDay().add(-1));

        assertEquals("Wrong number of prices in stream", 3, subjectPrices.streamByAgent(food, agent).collect(Collectors.toList()).size());
        assertEquals("Wrong number of prices in stream", 3, subjectPrices.streamByAgent(food.id.getAsLong(), agent.id.getAsLong()).collect(Collectors.toList()).size());
        assertEquals(
                "Wrong number of prices in stream (with order)",
                3,
                subjectPrices.streamByAgent(food, agent, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order)",
                3,
                subjectPrices.streamByAgent(food.id.getAsLong(), agent.id.getAsLong(), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order and limit)",
                2,
                subjectPrices.streamByAgent(food, agent, OptLimit.createLimit(2), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order and limit)",
                2,
                subjectPrices.streamByAgent(food.id.getAsLong(), agent.id.getAsLong(), OptLimit.createLimit(2), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version)", 3, subjectPrices.streamByAgent(food.name, agent.name).collect(Collectors.toList()).size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version) (with order)",
                3,
                subjectPrices.streamByAgent(food.name, agent.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version) (with order and limit)",
                2,
                subjectPrices.streamByAgent(food.name, agent.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(2))
                        .collect(Collectors.toList())
                        .size()
        );

        assertEquals(
                "Not latest price though ordered DESC",
                latest,
                subjectPrices.streamByAgent(food, agent, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not latest price though ordered DESC (String API version)",
                latest,
                subjectPrices.streamByAgent(food.name, agent.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(1))
                        .findFirst().get()
        );
        assertEquals(
                "Not farthest price though ordered ASC",
                farthest,
                subjectPrices.streamByAgent(food, agent, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not farthest price though ordered ASC (String API version)",
                farthest,
                subjectPrices.streamByAgent(food.name, agent.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC), OptLimit.createLimit(1))
                        .findFirst().get()
        );

        final SubjectPrice price = SubjectPrice.builder()
                .setPrice(Money.of(Units.RUB, BigDecimal.valueOf(1000L)))
                .setDay(new UtcDay().add(5))
                .setSubject(food)
                .setAgent(agent)
                .build();

        subjectPrices.register(price);


        assertEquals(
                "Not highest price though ordered DESC",
                price,
                subjectPrices.streamByAgent(food, agent, new OrderBy<>(SubjectPriceRepository.Field.PRICE, Order.DESC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not highest price though ordered DESC (String API version)",
                price,
                subjectPrices.streamByAgent(food.name, agent.name, new OrderBy<>(SubjectPriceRepository.Field.PRICE, Order.DESC), OptLimit.createLimit(1))
                        .findFirst().get()
        );
    }

    public void testCount() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final FundsMutationAgent agent2 = bundle.fundsMutationAgents().addAgent(
                FundsMutationAgent.builder()
                        .setName("Test2")
                        .setDescription("Something")
                        .build()
        );

        registerSome(subjectPrices, food, agent, new UtcDay());
        registerSome(subjectPrices, food, agent, new UtcDay().add(1));
        registerSome(subjectPrices, food, agent2, new UtcDay().add(1));
        registerSome(subjectPrices, food, agent2, new UtcDay());

        assertEquals("Wrong number of prices", 4, subjectPrices.count(food));
        assertEquals("Wrong number of prices (String counter edition)", 4, subjectPrices.count(food.name));
    }

    public void testStream() throws Exception {
        final SubjectPriceRepository subjectPrices = bundle.subjectPrices();

        final FundsMutationSubject food = TestUtils.getSubject(bundle, "Food");
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final FundsMutationAgent agent2 = bundle.fundsMutationAgents().addAgent(
                FundsMutationAgent.builder()
                        .setName("Test2")
                        .setDescription("Something")
                        .build()
        );

        registerSome(subjectPrices, food, agent, new UtcDay());
        registerSome(subjectPrices, food, agent2, new UtcDay().add(-1));
        registerSome(subjectPrices, food, agent2, new UtcDay().add(1));
        final SubjectPrice latest = registerSome(subjectPrices, food, agent, new UtcDay().add(10));
        final SubjectPrice farthest = registerSome(subjectPrices, food, agent, new UtcDay().add(-10));

        assertEquals("Wrong number of prices in stream", 5, subjectPrices.stream(food).collect(Collectors.toList()).size());
        assertEquals("Wrong number of prices in stream", 5, subjectPrices.stream(food.id.getAsLong()).collect(Collectors.toList()).size());
        assertEquals(
                "Wrong number of prices in stream (with order)",
                5,
                subjectPrices.stream(food, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order)",
                5,
                subjectPrices.stream(food.id.getAsLong(), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order and limit)",
                4,
                subjectPrices.stream(food, OptLimit.createLimit(4), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (with order and limit)",
                4,
                subjectPrices.stream(food.id.getAsLong(), OptLimit.createLimit(4), new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version)", 5, subjectPrices.stream(food.name).collect(Collectors.toList()).size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version) (with order)",
                5,
                subjectPrices.stream(food.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC))
                        .collect(Collectors.toList())
                        .size()
        );
        assertEquals(
                "Wrong number of prices in stream (String API version) (with order and limit)",
                4,
                subjectPrices.stream(food.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(4))
                        .collect(Collectors.toList())
                        .size()
        );

        assertEquals(
                "Not latest price though ordered DESC",
                latest,
                subjectPrices.stream(food, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not latest price though ordered DESC (String API version)",
                latest,
                subjectPrices.stream(food.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.DESC), OptLimit.createLimit(1))
                        .findFirst().get()
        );
        assertEquals(
                "Not farthest price though ordered ASC",
                farthest,
                subjectPrices.stream(food, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not farthest price though ordered ASC (String API version)",
                farthest,
                subjectPrices.stream(food.name, new OrderBy<>(SubjectPriceRepository.Field.DAY, Order.ASC), OptLimit.createLimit(1))
                        .findFirst().get()
        );

        final SubjectPrice price = SubjectPrice.builder()
                .setPrice(Money.of(Units.RUB, BigDecimal.valueOf(1000L)))
                .setDay(new UtcDay().add(5))
                .setSubject(food)
                .setAgent(agent)
                .build();

        subjectPrices.register(price);


        assertEquals(
                "Not highest price though ordered DESC",
                price,
                subjectPrices.stream(food, new OrderBy<>(SubjectPriceRepository.Field.PRICE, Order.DESC), OptLimit.createLimit(1)).findFirst().get()
        );
        assertEquals(
                "Not highest price though ordered DESC (String API version)",
                price,
                subjectPrices.stream(food.name, new OrderBy<>(SubjectPriceRepository.Field.PRICE, Order.DESC), OptLimit.createLimit(1))
                        .findFirst().get()
        );
    }

    private SubjectPrice registerSome(SubjectPriceRepository subjectPrices, FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day) {
        final SubjectPrice price = SubjectPrice.builder()
                .setPrice(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setDay(day)
                .setSubject(subject)
                .setAgent(agent)
                .build();

        subjectPrices.register(price);
        return price;
    }

}
