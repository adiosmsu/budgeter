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

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        BalanceAccount accountRub;
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

        mutationEventRepository.register(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
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

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            food = subjectRepository.addSubject(food);
        } catch (Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        BalanceAccount accountRub;
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

        mutationEventRepository.register(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, mutationEventRepository.getById(mutationEventRepository.currentSeqValue()).get());
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(subjectRepository).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            mutationEventRepository.register(test);
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
                .setQuantity(1)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-100L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setQuantity(1)
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

}
