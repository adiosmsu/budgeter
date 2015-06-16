package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.UtcDay;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Date: 6/15/15
 * Time: 6:43 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedFundsMutationEventPseudoTableTest {

    @Test
    public void testRememberPostponedExchangeableBenefit() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }

        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(666L)))
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final int id = Schema.POSTPONED_FUNDS_MUTATION_EVENTS.idSequence.get();
        assertEquals("Wrong remembered event", Money.of(rub, BigDecimal.valueOf(666L)), Schema.POSTPONED_FUNDS_MUTATION_EVENTS.get(id).mutationEvent.amount);
    }

    @Test
    public void testRememberPostponedExchangeableLoss() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }

        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(777L)))
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        final int id = Schema.POSTPONED_FUNDS_MUTATION_EVENTS.idSequence.get();
        assertEquals("Wrong remembered event", Money.of(rub, BigDecimal.valueOf(777L)), Schema.POSTPONED_FUNDS_MUTATION_EVENTS.get(id).mutationEvent.amount);
    }

    @Test
    public void testStreamRememberedBenefits() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }

        final OffsetDateTime ts = OffsetDateTime.of(1998, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(888L)))
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(999L)))
                .setTimestamp(ts)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedBenefits(new UtcDay(ts), rub, CurrencyUnit.USD).collect(Collectors.toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", collected.get(0).mutationEvent.amount, Money.of(rub, BigDecimal.valueOf(888L)));

        final long count =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS
                        .streamRememberedBenefits(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

    @Test
    public void testStreamRememberedLosses() throws Exception {
        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(() -> new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore));
        }

        final OffsetDateTime ts = OffsetDateTime.of(1997, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(1000L)))
                .setTimestamp(ts)
                .build();
        final FundsMutationEvent breadBuy2 = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(rub, BigDecimal.valueOf(1001L)))
                .setTimestamp(ts)
                .build();
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());
        Schema.POSTPONED_FUNDS_MUTATION_EVENTS.rememberPostponedExchangeableLoss(breadBuy2, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        final List<PostponedFundsMutationEventRepository.PostponedMutationEvent> collected =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS.streamRememberedLosses(new UtcDay(ts), rub, CurrencyUnit.USD).collect(Collectors.toList());
        assertEquals(collected.size(), 1);
        assertEquals("Wrong event streamed", collected.get(0).mutationEvent.amount, Money.of(rub, BigDecimal.valueOf(1001L)));

        final long count =
                Schema.POSTPONED_FUNDS_MUTATION_EVENTS
                        .streamRememberedLosses(new UtcDay(OffsetDateTime.of(1971, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.USD, CurrencyUnit.EUR).count();
        assertEquals(count, 0);
    }

}