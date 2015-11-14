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

import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.data.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/26/15
 * Time: 6:27 PM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedFundsMutationEventRepoTester {

    private static final UtcDay BOGUS_DAY = new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));

    private final Bundle bundle;

    public PostponedFundsMutationEventRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setUp() {
        bundle.clearSchema();
    }

    public void testRememberPostponedExchangeableBenefit() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (final Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(666L)))
                .setRelevantBalance(TestUtils.prepareBalance(bundle, Units.RUB))
                .setAgent(agent)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final Long id = postMutRepo.currentSeqValue();
        assertEquals("Wrong remembered event", Money.of(Units.RUB, BigDecimal.valueOf(666L)), postMutRepo.getById(id).get().mutationEvent.amount);
    }

    public void testRememberPostponedExchangeableLoss() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (final Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-777L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final Long id = postMutRepo.currentSeqValue();
        assertEquals("Wrong remembered event", Money.of(Units.RUB, BigDecimal.valueOf(-777L)), postMutRepo.getById(id).get().mutationEvent.amount);
    }

    public void testStreamRememberedBenefits() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.of(1998, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        rememberBenefits(ts);

        final List<PostponedMutationEvent> collected =
                bundle.postponedFundsMutationEvents().streamRememberedBenefits(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).collect(Collectors.<PostponedMutationEvent>toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", Money.of(Units.RUB, BigDecimal.valueOf(888L)), collected.get(0).mutationEvent.amount);

        final long count = bundle.postponedFundsMutationEvents().streamRememberedBenefits(BOGUS_DAY, CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

    private void rememberBenefits(OffsetDateTime ts) throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (final Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);

        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(888L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-999L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());
    }

    public void testStreamRememberedLosses() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.of(1997, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        rememberLosses(ts);

        final List<PostponedMutationEvent> collected =
                bundle.postponedFundsMutationEvents().streamRememberedLosses(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).collect(Collectors.<PostponedMutationEvent>toList());
        assertEquals(1, collected.size());
        assertEquals("Wrong event streamed", Money.of(Units.RUB, BigDecimal.valueOf(-1001L)), collected.get(0).mutationEvent.amount);

        final long count = bundle.postponedFundsMutationEvents().streamRememberedLosses(BOGUS_DAY, CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(0, count);
    }

    private void rememberLosses(OffsetDateTime ts) throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (final Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);

        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(1000L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setPortion(BigDecimal.TEN)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-1001L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());
    }

    public void testStreamRememberedEvents() throws Exception {
        final OffsetDateTime ts = OffsetDateTime.of(1997, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        rememberBenefits(ts);
        rememberLosses(ts);

        final List<PostponedMutationEvent> collected =
                bundle.postponedFundsMutationEvents().streamRememberedEvents(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).collect(Collectors.<PostponedMutationEvent>toList());
        assertEquals(4, collected.size());
        assertEquals(0, bundle.postponedFundsMutationEvents().streamRememberedEvents(BOGUS_DAY, CurrencyUnit.USD, CurrencyUnit.EUR).count());
    }

}
