package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Date: 6/15/15
 * Time: 6:43 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventPseudoTableTest {

    @Before
    public void setUp() {
        Schema.clearSchema();
    }

    @Test
    public void testRememberPostponedExchangeableBenefit() throws Exception {
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(666L)))
                .setRelevantBalance(TestUtils.prepareBalance(Units.RUB))
                .setAgent(agent)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final int id = Schema.POSTPONED_FUNDS_MUTATION_EVENTS.idSequence.get();
        assertEquals("Wrong remembered event", Money.of(Units.RUB, BigDecimal.valueOf(666L)), Schema.POSTPONED_FUNDS_MUTATION_EVENTS.get(id).obj.mutationEvent.amount);
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(777L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final int id = Schema.POSTPONED_FUNDS_MUTATION_EVENTS.idSequence.get();
        assertEquals("Wrong remembered event", Money.of(Units.RUB, BigDecimal.valueOf(777L)), Schema.POSTPONED_FUNDS_MUTATION_EVENTS.get(id).obj.mutationEvent.amount);
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(Units.RUB);
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);

        final OffsetDateTime ts = OffsetDateTime.of(1998, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(888L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(999L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedBenefits(new UtcDay(ts), Units.RUB, CurrencyUnit.USD)
                        .collect(Collectors.<PostponedFundsMutationEventRepository.PostponedMutationEvent>toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", collected.get(0).mutationEvent.amount, Money.of(Units.RUB, BigDecimal.valueOf(888L)));

        final long count =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS
                        .streamRememberedBenefits(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(Units.RUB);
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);

        final OffsetDateTime ts = OffsetDateTime.of(1997, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(1000L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(1001L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .setTimestamp(ts)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedLosses(new UtcDay(ts), Units.RUB, CurrencyUnit.USD)
                        .collect(Collectors.<PostponedFundsMutationEventRepository.PostponedMutationEvent>toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", collected.get(0).mutationEvent.amount, Money.of(Units.RUB, BigDecimal.valueOf(1001L)));

        final long count =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS
                        .streamRememberedLosses(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

}