package ru.adios.budgeter.api;

import java8.util.function.Supplier;
import org.joda.money.Money;

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
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        Treasury.BalanceAccount accountRub;
        try {
            accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        } catch (Exception ignore) {
            accountRub = bundle.treasury().getAccountForName("accountRUB").get();
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        mutationEventRepository.registerBenefit(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(subjectRepository).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            mutationEventRepository.registerBenefit(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    public void testRegisterLoss() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

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
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        Treasury.BalanceAccount accountRub;
        try {
            accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        } catch (Exception ignore) {
            accountRub = bundle.treasury().getAccountForName("accountRUB").get();
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final FundsMutationEventRepository mutationEventRepository = bundle.fundsMutationEvents();

        mutationEventRepository.registerLoss(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(subjectRepository).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            mutationEventRepository.registerLoss(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
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
                        .streamMutationEvents(new OrderBy<FundsMutationEventRepository.Field>(FundsMutationEventRepository.Field.AMOUNT, Order.ASC))
                        .findFirst()
                        .get()
                        .amount
                        .isNegative()
        );
        assertTrue(mutationEventRepository
                        .streamMutationEvents(new OrderBy<FundsMutationEventRepository.Field>(FundsMutationEventRepository.Field.AMOUNT, Order.DESC))
                        .findFirst()
                        .get()
                        .amount
                        .isPositive()
        );
    }

}
