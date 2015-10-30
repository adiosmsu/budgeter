package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/26/15
 * Time: 6:17 PM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedCurrencyExchangeEventRepoTester {

    private final Bundle bundle;
    private FundsMutationAgent agent = FundsMutationAgent.builder().setName("Tesy").build();

    public PostponedCurrencyExchangeEventRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setUp() {
        bundle.clearSchema();
        final TransactionalSupport txs = bundle.getTransactionalSupport();
        if (txs != null) {
            txs.runWithTransaction(new Runnable() {
                @Override
                public void run() {
                    agent = bundle.fundsMutationAgents().addAgent(agent);
                }
            });
        } else {
            agent = bundle.fundsMutationAgents().addAgent(agent);
        }
    }

    public void testRememberPostponedExchange() throws Exception {
        final PostponedCurrencyExchangeEventRepository postExRepo = bundle.postponedCurrencyExchangeEvents();

        postExRepo.rememberPostponedExchange(BigDecimal.valueOf(1034530L), TestUtils.prepareBalance(bundle, CurrencyUnit.EUR),
                TestUtils.prepareBalance(bundle, CurrencyUnit.USD), Optional.of(BigDecimal.valueOf(0.89)), OffsetDateTime.now(), agent);
        final Long id = postExRepo.currentSeqValue();
        assertEquals("Money don't match", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1034530L)), Money.of(CurrencyUnit.EUR, postExRepo.getById(id).get().toBuy));
    }

    public void testStreamRememberedExchanges() throws Exception {
        final PostponedCurrencyExchangeEventRepository postExRepo = bundle.postponedCurrencyExchangeEvents();

        postExRepo.rememberPostponedExchange(BigDecimal.valueOf(1000L), TestUtils.prepareBalance(bundle, CurrencyUnit.EUR), TestUtils.prepareBalance(bundle, CurrencyUnit.USD),
                Optional.of(BigDecimal.valueOf(0.89)), OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), agent);
        postExRepo.streamRememberedExchanges(new UtcDay(OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), CurrencyUnit.EUR, CurrencyUnit.USD).forEach(
                new Consumer<PostponedCurrencyExchangeEventRepository.PostponedExchange>() {
                    @Override
                    public void accept(PostponedCurrencyExchangeEventRepository.PostponedExchange postponedExchange) {
                        assertEquals("Wrong stream: " + postponedExchange.toBuy, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), Money.of(CurrencyUnit.EUR, postponedExchange.toBuy));
                    }
                }
        );
    }

}
