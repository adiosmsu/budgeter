package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 10/26/15
 * Time: 6:12 PM
 *
 * @author Mikhail Kulikov
 */
public final class AccounterTester {

    private final Bundle bundle;

    public AccounterTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testStreamAllPostponingReasons() throws Exception {
        testStreamAllPostponingReasons(false);
    }

    public void testStreamAllPostponingReasonsCompat() throws Exception {
        testStreamAllPostponingReasons(true);
    }

    private void testStreamAllPostponingReasons(boolean compat) throws Exception {
        bundle.clearSchema();

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
        final BalanceAccount accountRub = TestUtils.prepareBalance(bundle, Units.RUB);
        final BalanceAccount accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final BalanceAccount accountAud = TestUtils.prepareBalance(bundle, CurrencyUnit.AUD);
        final FundsMutationEvent breadBuy = FundsMutationEvent.builder()
                .setQuantity(10)
                .setSubject(food)
                .setAmount(Money.of(Units.RUB, BigDecimal.valueOf(50L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        final PostponedFundsMutationEventRepository postMutRepo = bundle.accounter().postponedFundsMutationEventRepository();
        postMutRepo.rememberPostponedExchangeableEvent(breadBuy, CurrencyUnit.USD, Optional.<BigDecimal>empty());

        FundsMutationSubject game = FundsMutationSubject.builder(subjectRepository).setName("Game").setType(FundsMutationSubject.Type.PRODUCT).build();
        game = subjectRepository.addSubject(game);
        final FundsMutationEvent gameBuy = FundsMutationEvent.builder()
                .setQuantity(1)
                .setSubject(game)
                .setAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(-10L)))
                .setRelevantBalance(accountRub)
                .setAgent(agent)
                .build();

        postMutRepo.rememberPostponedExchangeableEvent(gameBuy, Units.RUB, Optional.<BigDecimal>empty());

        final UtcDay today = new UtcDay();
        final UtcDay daySecond = new UtcDay(today.inner.minus(1, ChronoUnit.DAYS));
        final FundsMutationEvent gameBuy2 = FundsMutationEvent.builder()
                .setQuantity(1)
                .setSubject(game)
                .setAmount(Money.of(CurrencyUnit.GBP, BigDecimal.valueOf(-10L)))
                .setRelevantBalance(accountAud)
                .setTimestamp(daySecond.inner)
                .setAgent(agent)
                .build();

        postMutRepo.rememberPostponedExchangeableEvent(gameBuy2, CurrencyUnit.AUD, Optional.<BigDecimal>empty());

        bundle.accounter().postponedCurrencyExchangeEventRepository()
                .rememberPostponedExchange(BigDecimal.valueOf(100L), accountEur, accountRub, Optional.of(BigDecimal.valueOf(54.23)), OffsetDateTime.now(), agent);

        final List<Accounter.PostponingReasons> collected = bundle.accounter().streamAllPostponingReasons(compat).collect(Collectors.<Accounter.PostponingReasons>toList());
        assertEquals("List size don't match: " + collected.size(), 2, collected.size());

        final Accounter.PostponingReasons p1 = collected.get(0);
        final Accounter.PostponingReasons p2 = collected.get(1);

        if (!today.equals(p1.dayUtc) && !today.equals(p2.dayUtc)) {
            fail("Expected timestamp " + today + " don't match either of " + p1.dayUtc + " and " + p2.dayUtc);
        }
        if (!daySecond.equals(p1.dayUtc) && !daySecond.equals(p2.dayUtc)) {
            fail("Expected timestamp " + daySecond + " don't match either of " + p1.dayUtc + " and " + p2.dayUtc);
        }

        final ImmutableSet<CurrencyUnit> expectedFirst = ImmutableSet.of(CurrencyUnit.EUR, CurrencyUnit.USD, Units.RUB);
        final ImmutableSet<CurrencyUnit> expectedSecond = ImmutableSet.of(CurrencyUnit.AUD, CurrencyUnit.GBP);
        if (!expectedFirst.equals(p1.sufferingUnits) && !expectedFirst.equals(p2.sufferingUnits)) {
            fail("Expected set " + expectedFirst + " don't match either of " + p1.sufferingUnits + " and " + p2.sufferingUnits);
        }
        if (!expectedSecond.equals(p1.sufferingUnits) && !expectedSecond.equals(p2.sufferingUnits)) {
            fail("Expected set " + expectedSecond + " don't match either of " + p1.sufferingUnits + " and " + p2.sufferingUnits);
        }
    }

    public void testStreamAllPostponingReasonsEmptyCompat() throws Exception {
        testStreamAllPostponingReasonsEmpty(true);
    }

    public void testStreamAllPostponingReasonsEmpty() throws Exception {
        testStreamAllPostponingReasonsEmpty(false);
    }

    private void testStreamAllPostponingReasonsEmpty(boolean compat) throws Exception {
        bundle.clearSchema();
        final List<Accounter.PostponingReasons> collected =
                bundle.accounter().streamAllPostponingReasons(compat).collect(Collectors.<Accounter.PostponingReasons>toList());
        assertEquals("Non-empty postponed on cleared DB", 0, collected.size());
    }

}
