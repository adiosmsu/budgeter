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
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Date: 10/26/15
 * Time: 4:44 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationEventRepoTester {

    private final Bundle bundle;

    public FundsMutationEventRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setUp() {
        bundle.clearSchema();
    }

    public void testRegisterBenefit() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final FundsMutationSubject food = getFoodSubject(subjectRepository);
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.valueOf(10))
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        mutationEventRepository.register(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setPortion(BigDecimal.valueOf(10))
                    .setSubject(FundsMutationSubject.builder(subjectRepository).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            mutationEventRepository.register(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    public void testRegisterLoss() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final FundsMutationSubject food = getFoodSubject(subjectRepository);
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.valueOf(10))
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        mutationEventRepository.register(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setPortion(BigDecimal.valueOf(10))
                    .setSubject(FundsMutationSubject.builder(subjectRepository).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            mutationEventRepository.register(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    public void testCount() throws Exception {
        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();
        testRegisterLoss();
        testRegisterBenefit();
        assertEquals("Wrong number of records", 2, mutationEventRepository.countMutationEvents());

        final FundsMutationSubject food = getFoodSubject(bundle.fundsMutationSubjects());
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final FundsMutationEvent someBuy = FundsMutationEvent.builder()
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-10L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();
        mutationEventRepository.register(someBuy);

        assertEquals("Wrong number of records", 3, mutationEventRepository.countMutationEvents());
    }

    public void testStream() throws Exception {
        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        testRegisterLoss();
        testRegisterBenefit();
        assertEquals(1, mutationEventRepository.streamMutationEvents(OptLimit.createLimit(1)).count());
        assertEquals(1, mutationEventRepository.streamMutationEvents(OptLimit.createOffset(1)).count());
        assertEquals(1, mutationEventRepository.streamMutationEvents(OptLimit.create(1, 1)).count());
        assertEquals(0, mutationEventRepository.streamMutationEvents(OptLimit.createOffset(2)).count());
        assertTrue(mutationEventRepository
                        .streamMutationEvents(new OrderBy<>(FundsMutationEventRepository.Field.AMOUNT, Order.ASC))
                        .findFirst()
                        .get()
                        .amount
                        .isNegative()
        );
        assertTrue(mutationEventRepository
                        .streamMutationEvents(new OrderBy<>(FundsMutationEventRepository.Field.AMOUNT, Order.DESC))
                        .findFirst()
                        .get()
                        .amount
                        .isPositive()
        );
    }

    public void testStreamForDay() throws Exception {
        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        testRegisterLoss();
        testRegisterBenefit();

        BalanceAccount accountRub = bundle.treasury().getAccountForName("accountRUB").get();
        BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final FundsMutationAgent agent = bundle.fundsMutationAgents().findByName("Test").get();
        final FundsMutationSubject food = bundle.fundsMutationSubjects().findByName("Food").get();
        final FundsMutationEvent breadBuy1 = FundsMutationEvent.builder()
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-100L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setSubject(food)
                .setAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(-10L)))
                .setRelevantBalance(accountRub)
                .setTimestamp(new UtcDay().add(-10).inner)
                .setAgent(agent)
                .build();

        bundle.fundsMutationEvents().register(breadBuy1);
        bundle.fundsMutationEvents().register(breadBuy2);

        assertEquals("Stream for day counted wrong", 3, bundle.fundsMutationEvents().streamForDay(new UtcDay()).count());
    }

    private FundsMutationSubject getFoodSubject(FundsMutationSubjectRepository subjectRepository) {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        return food;
    }

}
