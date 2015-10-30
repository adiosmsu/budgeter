package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;

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
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(666L)))
                .setRelevantBalance(TestUtils.prepareBalance(bundle, Units.RUB))
                .setAgent(agent)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
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

        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(777L)))
                .setRelevantBalance(accountUsd)
                .setAgent(agent)
                .build();
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableLoss(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final Long id = postMutRepo.currentSeqValue();
        assertEquals("Wrong remembered event", Money.of(Units.RUB, BigDecimal.valueOf(-777L)), postMutRepo.getById(id).get().mutationEvent.amount);
    }

    public void testStreamRememberedBenefits() throws Exception {
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
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);

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
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        postMutRepo.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                postMutRepo.streamRememberedBenefits(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).collect(Collectors.<PostponedFundsMutationEventRepository.PostponedMutationEvent>toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", collected.get(0).mutationEvent.amount, Money.of(Units.RUB, BigDecimal.valueOf(888L)));

        final long count = postMutRepo.streamRememberedBenefits(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

    public void testStreamRememberedLosses() throws Exception {
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
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);

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
        final PostponedFundsMutationEventRepository postMutRepo = bundle.postponedFundsMutationEvents();
        postMutRepo.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        postMutRepo.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                postMutRepo.streamRememberedLosses(new UtcDay(ts), Units.RUB, CurrencyUnit.USD).collect(Collectors.<PostponedFundsMutationEventRepository.PostponedMutationEvent>toList());
        assertEquals(1, collected.size());
        assertEquals("Wrong event streamed", Money.of(Units.RUB, BigDecimal.valueOf(-1001L)), collected.get(0).mutationEvent.amount);

        final long count = postMutRepo.streamRememberedLosses(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(0, count);
    }

}
