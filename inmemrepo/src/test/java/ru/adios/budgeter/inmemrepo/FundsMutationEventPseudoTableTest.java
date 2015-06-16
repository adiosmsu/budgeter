package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationSubject;

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

    @Test
    public void testRegisterBenefit() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(CurrencyUnit.of("RUB"), BigDecimal.valueOf(50L)))
                .build();
        Schema.FUNDS_MUTATION_EVENTS.registerBenefit(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, Schema.FUNDS_MUTATION_EVENTS.get(Schema.FUNDS_MUTATION_EVENTS.idSequence.get()).obj);
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Test").setType(FundsMutationSubject.SubjectType.PRODUCT).build())
                    .setAmount(Money.of(CurrencyUnit.of("RUB"), BigDecimal.valueOf(50L)))
                    .build();
            Schema.FUNDS_MUTATION_EVENTS.registerBenefit(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

    @Test
    public void testRegisterLoss() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(CurrencyUnit.of("RUB"), BigDecimal.valueOf(50L)))
                .build();
        Schema.FUNDS_MUTATION_EVENTS.registerLoss(breadBuy);
        assertEquals("No breadBuy event found", breadBuy, Schema.FUNDS_MUTATION_EVENTS.get(Schema.FUNDS_MUTATION_EVENTS.idSequence.get()).obj);
        try {
            final FundsMutationEvent test = FundsMutationEvent.builder()
                    .setQuantity(10)
                    .setSubject(FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Test").setType(FundsMutationSubject.SubjectType.PRODUCT).build())
                    .setAmount(Money.of(CurrencyUnit.of("RUB"), BigDecimal.valueOf(50L)))
                    .build();
            Schema.FUNDS_MUTATION_EVENTS.registerLoss(test);
            fail("Subject existence test failed");
        } catch (Exception ignore) {}
    }

}