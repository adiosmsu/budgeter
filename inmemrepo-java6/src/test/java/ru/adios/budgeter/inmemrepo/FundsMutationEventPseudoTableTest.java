package ru.adios.budgeter.inmemrepo;

import java8.util.function.Supplier;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(Units.RUB);
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
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<IllegalStateException>() {
                @Override
                public IllegalStateException get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }
        final FundsMutationAgent agent = TestUtils.prepareTestAgent();
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(Units.RUB);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
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

}