package ru.adios.budgeter.inmemrepo;

import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Date: 6/15/15
 * Time: 1:19 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationEventPseudoTableTest {

    @Before
    public void setUp() {
        Schema.clearSchema();
    }

    @Test
    public void testRegisterBenefit() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();
        Treasury.BalanceAccount accountRub;
        try {
            accountRub = TestUtils.prepareBalance(Units.RUB);
        } catch (Exception ignore) {
            accountRub = Schema.TREASURY.getAccountForName("accountRUB").get();
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();
        Schema.FUNDS_MUTATION_EVENTS.registerBenefit(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, Schema.FUNDS_MUTATION_EVENTS.get(Schema.FUNDS_MUTATION_EVENTS.idSequence.get()).obj);
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            Schema.FUNDS_MUTATION_EVENTS.registerBenefit(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    @Test
    public void testRegisterLoss() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();
        Treasury.BalanceAccount accountRub;
        try {
            accountRub = TestUtils.prepareBalance(Units.RUB);
        } catch (Exception ignore) {
            accountRub = Schema.TREASURY.getAccountForName("accountRUB").get();
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(-50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();
        Schema.FUNDS_MUTATION_EVENTS.registerLoss(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, Schema.FUNDS_MUTATION_EVENTS.get(Schema.FUNDS_MUTATION_EVENTS.idSequence.get()).obj);
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Test").setType(FundsMutationSubject.Type.PRODUCT).build())
                    .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                    .setRelevantBalance(accountRub)
                    .build();
            Schema.FUNDS_MUTATION_EVENTS.registerLoss(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    @Test
    public void testStream() throws Exception {
        testRegisterLoss();
        testRegisterBenefit();
        assertEquals(1, Schema.FUNDS_MUTATION_EVENTS.stream(OptLimit.createLimit(1)).count());
        assertEquals(1, Schema.FUNDS_MUTATION_EVENTS.stream(OptLimit.createOffset(1)).count());
        assertEquals(1, Schema.FUNDS_MUTATION_EVENTS.stream(OptLimit.create(1, 1)).count());
        assertEquals(0, Schema.FUNDS_MUTATION_EVENTS.stream(OptLimit.createOffset(2)).count());
        assertTrue(Schema.FUNDS_MUTATION_EVENTS
                        .stream(new OrderBy<>(FundsMutationEventRepository.Field.AMOUNT, Order.ASC))
                        .findFirst()
                        .get()
                        .amount
                        .isNegative()
        );
        assertTrue(Schema.FUNDS_MUTATION_EVENTS
                        .stream(new OrderBy<>(FundsMutationEventRepository.Field.AMOUNT, Order.DESC))
                        .findFirst()
                        .get()
                        .amount
                        .isPositive()
        );
    }

}