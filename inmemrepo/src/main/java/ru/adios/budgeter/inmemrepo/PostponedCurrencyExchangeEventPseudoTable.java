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

package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.PostponedExchange;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:52 AM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedCurrencyExchangeEventPseudoTable
        extends AbstractPseudoTable<Stored<PostponedExchange>, PostponedExchange>
        implements PostponedCurrencyExchangeEventRepository
{

    public static final PostponedCurrencyExchangeEventPseudoTable INSTANCE = new PostponedCurrencyExchangeEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<PostponedExchange>> table = new ConcurrentHashMap<>(100, 0.75f, 4);

    private PostponedCurrencyExchangeEventPseudoTable() {}

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public Optional<PostponedExchange> getById(Long id) {
        final Stored<PostponedExchange> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          BalanceAccount toBuyAccount,
                                          BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent)
    {
        final int id = idSequence.incrementAndGet();
        checkState(
                table.computeIfAbsent(id, integer -> new Stored<>(id, new PostponedExchange(OptionalLong.of(id), toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent, true)))
                        .id == id
        );
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.values().stream().filter(event -> {
            if (!event.obj.relevant) {
                return false;
            }
            final CurrencyUnit bu = event.obj.toBuyAccount.getUnit();
            final CurrencyUnit su = event.obj.sellAccount.getUnit();
            return day.equals(new UtcDay(event.obj.timestamp))
                    && (bu.equals(oneOf) || bu.equals(secondOf))
                    && (su.equals(oneOf) || su.equals(secondOf));
        }).map(storedPostponedExchangeEvent -> storedPostponedExchangeEvent.obj);
    }

    @Override
    public boolean markEventProcessed(PostponedExchange exchange) {
        checkArgument(exchange.id.isPresent());
        final int key = (int) exchange.id.getAsLong();
        final Stored<PostponedExchange> stored = table.get(key);
        final PostponedExchange old = stored.obj;
        return table.replace(
                key,
                stored,
                new Stored<>(
                        key,
                        new PostponedExchange(OptionalLong.of(key), old.toBuy, old.toBuyAccount, old.sellAccount, old.customRate, old.timestamp, old.agent, false)
                )
        );
    }

    Stream<PostponedExchange> streamAll() {
        return table.values().stream().map(storedPostponedExchangeEvent -> storedPostponedExchangeEvent.obj);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<PostponedExchange>> innerTable() {
        return table;
    }

}
