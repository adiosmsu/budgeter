package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/26/15
 * Time: 6:12 PM
 *
 * @author Mikhail Kulikov
 */
public final class AccounterTester {

    private final Bundle bundle;

    private Accounter accounter;

    public void setAccounter(Accounter accounter) {
        this.accounter = accounter;
    }

    public AccounterTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testStreamAllPostponingReasons() throws Exception {
        bundle.clearSchema();

        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject food;
        try {
            food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            subjectRepository.addSubject(food);
        } catch (final Exception ignore) {
            food = subjectRepository.findByName("Food").orElseThrow(new Supplier<Exception>() {
                @Override
                public Exception get() {
                    return new IllegalStateException("Unable to create Food and fetch it simultaneously", ignore);
                }
            });
        }

        final FundsMutationAgent agent = TestUtils.prepareTestAgent(bundle);
        final Treasury.BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final Treasury.BalanceAccount accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final PostponedFundsMutationEventRepository postMutRepo = accounter.postponedFundsMutationEventRepository();
        postMutRepo.rememberPostponedExchangeableBenefit(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        FundsMutationSubject game = FundsMutationSubject.builder(subjectRepository).setName("Game").setType(FundsMutationSubject.Type.PRODUCT).build();
        final FundsMutationEvent gameBuy = FundsMutationEvent.builder()
                .setQuantity(1)
                .setSubject(game)
                .setAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(10L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        postMutRepo.rememberPostponedExchangeableLoss(gameBuy, Units.RUB, Optional.<BigDecimal>empty());

        accounter.postponedCurrencyExchangeEventRepository()
                .rememberPostponedExchange(BigDecimal.valueOf(100L), accountEur, accountRub, Optional.of(BigDecimal.valueOf(54.23)), OffsetDateTime.now(), agent);

        final List<Accounter.PostponingReasons> collected = accounter.streamAllPostponingReasons().collect(Collectors.<Accounter.PostponingReasons>toList());
        assertEquals("Too large list: " + collected.size(), 1, collected.size());
        assertEquals("Problematic currencies sets don't match", ImmutableSet.of(CurrencyUnit.EUR, CurrencyUnit.USD, Units.RUB), collected.get(0).sufferingUnits);
    }

}
