package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.util.List;

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
        Schema.clearSchemaStatic();

        final InnerMemoryAccounter innerMemoryAccounter = new InnerMemoryAccounter();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (final Exception ignore) {
            food = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final FundsMutationAgent agent = TestUtils.prepareTestAgent();
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(Units.RUB);
        final Treasury.BalanceAccount accountEur = TestUtils.prepareBalance(CurrencyUnit.EUR);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final PostponedFundsMutationEventRepository postMutRepo = innerMemoryAccounter.postponedFundsMutationEventRepository();
        postMutRepo.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        FundsMutationSubject game = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Game").setType(FundsMutationSubject.Type.PRODUCT).build();
        final FundsMutationEvent gameBuy = FundsMutationEvent.builder()
                .setQuantity(1)
                .setSubject(game)
                .setAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(10L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        postMutRepo.rememberPostponedExchangeableLoss(gameBuy, Units.RUB, Optional.<BigDecimal>empty());

        innerMemoryAccounter.postponedCurrencyExchangeEventRepository()
                .rememberPostponedExchange(BigDecimal.valueOf(100L), accountEur, accountRub, Optional.of(BigDecimal.valueOf(54.23)), OffsetDateTime.now(), agent);

        final List<Accounter.PostponingReasons> collected = innerMemoryAccounter.streamAllPostponingReasons().collect(Collectors.<Accounter.PostponingReasons>toList());
        assertEquals("Too large list: " + collected.size(), 1, collected.size());
        assertEquals("Problematic currencies sets don't match", ImmutableSet.of(CurrencyUnit.EUR, CurrencyUnit.USD, Units.RUB), collected.get(0).sufferingUnits);
    }

}