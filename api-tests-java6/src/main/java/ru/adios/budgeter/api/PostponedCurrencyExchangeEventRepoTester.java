/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.PostponedExchange;

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
                new Consumer<PostponedExchange>() {
                    @Override
                    public void accept(PostponedExchange postponedExchange) {
                        assertEquals("Wrong stream: " + postponedExchange.toBuy, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000L)), Money.of(CurrencyUnit.EUR, postponedExchange.toBuy));
                    }
                }
        );
    }

}
