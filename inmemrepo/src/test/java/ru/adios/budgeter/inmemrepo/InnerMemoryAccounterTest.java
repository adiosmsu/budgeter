package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationSubject;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Date: 6/15/15
 * Time: 6:18 PM
 *
 * @author Mikhail Kulikov
 */
public class InnerMemoryAccounterTest {

    @Test
    public void testStreamAllPostponingReasons() throws Exception {
        Schema.clearSchema();

        final InnerMemoryAccounter innerMemoryAccounter = new InnerMemoryAccounter();

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
                .setAmount(Money.of(rub, BigDecimal.valueOf(50L)))
                .build();

        innerMemoryAccounter.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.empty());

        FundsMutationSubject game = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Game").setType(FundsMutationSubject.SubjectType.PRODUCT).build();
        final FundsMutationEvent gameBuy = FundsMutationEvent.builder()
                .setQuantity(1)
                .setSubject(game)
                .setAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(10L)))
                .build();

        innerMemoryAccounter.rememberPostponedExchangeableLoss(gameBuy, rub, Optional.empty());

        innerMemoryAccounter.rememberPostponedExchange(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(100L)), rub, Optional.of(BigDecimal.valueOf(54.23)), OffsetDateTime.now());

        final List<Accounter.PostponingReasons> collected = innerMemoryAccounter.streamAllPostponingReasons().collect(Collectors.toList());
        assertEquals("Too large list: " + collected.size(), 1, collected.size());
        assertEquals("Problematic currencies sets don't match", ImmutableSet.of(CurrencyUnit.EUR, CurrencyUnit.USD, rub), collected.get(0).sufferingUnits);
    }

}