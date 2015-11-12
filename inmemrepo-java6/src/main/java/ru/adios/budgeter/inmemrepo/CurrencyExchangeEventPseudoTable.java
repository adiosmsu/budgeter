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

import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:08 AM
 *
 * @author Mikhail Kulikov
 */
public final class CurrencyExchangeEventPseudoTable extends AbstractPseudoTable<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent> implements CurrencyExchangeEventRepository {

    public static final CurrencyExchangeEventPseudoTable INSTANCE = new CurrencyExchangeEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> table = new ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>>(100, 0.75f, 4);
    private final Default def = new Default(this);

    private CurrencyExchangeEventPseudoTable() {}

    @Override
    public Optional<CurrencyExchangeEvent> getById(Long id) {
        final Stored<CurrencyExchangeEvent> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        final int id = idSequence.incrementAndGet();
        checkState(table.putIfAbsent(id, new Stored<CurrencyExchangeEvent>(id, exchangeEvent)) == null);
    }

    @Override
    public int countExchangeEvents() {
        return table.size();
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(final List<OrderBy<Field>> options, final Optional<OptLimit> limitRef) {
        return table.values()
                .getStream()
                .map(new Function<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent>() {
                    @Override
                    public CurrencyExchangeEvent apply(Stored<CurrencyExchangeEvent> stored) {
                        return constructValid(stored);
                    }
                })
                .sorted(new Comparator<CurrencyExchangeEvent>() {
                    @Override
                    public int compare(CurrencyExchangeEvent e1, CurrencyExchangeEvent e2) {
                        int res = 1;
                        for (final OrderBy<Field> opt : options) {
                            switch (opt.field) {
                                case TIMESTAMP:
                                    res = opt.order.applyToCompareResult(e1.timestamp.compareTo(e2.timestamp));
                                    if (res < 0) {
                                        return -1;
                                    }
                                    break;
                            }
                        }
                        return res;
                    }
                })
                .filter(new LimitingPredicate<CurrencyExchangeEvent>(limitRef));
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        return def.streamExchangeEvents(options);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> innerTable() {
        return table;
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamForDay(final UtcDay day) {
        return StreamSupport.stream(table.values().getSpliterator(), false)
                .filter(new Predicate<Stored<CurrencyExchangeEvent>>() {
                    @Override
                    public boolean test(Stored<CurrencyExchangeEvent> stored) {
                        return new UtcDay(stored.obj.timestamp).equals(day);
                    }
                })
                .map(new Function<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent>() {
                    @Override
                    public CurrencyExchangeEvent apply(Stored<CurrencyExchangeEvent> stored) {
                        return constructValid(stored);
                    }
                });
    }

    private CurrencyExchangeEvent constructValid(Stored<CurrencyExchangeEvent> stored) {
        return CurrencyExchangeEvent.builder().setEvent(stored.obj)
                .setBoughtAccount(Schema.TREASURY.getAccountForName(stored.obj.boughtAccount.name).get())
                .setSoldAccount(Schema.TREASURY.getAccountForName(stored.obj.soldAccount.name).get())
                .build();
    }

}
