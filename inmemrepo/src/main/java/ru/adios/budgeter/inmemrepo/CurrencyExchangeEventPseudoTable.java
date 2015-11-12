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

import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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

    private final ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> table = new ConcurrentHashMap<>(100, 0.75f, 4);

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
        checkState(table.putIfAbsent(id, new Stored<>(id, exchangeEvent)) == null);
    }

    @Override
    public int countExchangeEvents() {
        return table.size();
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, Optional<OptLimit> limitRef) {
        return table.values()
                .stream()
                .map(this::constructValid)
                .sorted((e1, e2) -> {
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
                })
                .filter(new LimitingPredicate<>(limitRef));
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> innerTable() {
        return table;
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.values().stream()
                .filter(stored -> new UtcDay(stored.obj.timestamp).equals(day))
                .map(this::constructValid);
    }

    private CurrencyExchangeEvent constructValid(Stored<CurrencyExchangeEvent> stored) {
        return CurrencyExchangeEvent.builder().setEvent(stored.obj)
                .setBoughtAccount(Schema.TREASURY.getAccountForName(stored.obj.boughtAccount.name).get())
                .setSoldAccount(Schema.TREASURY.getAccountForName(stored.obj.soldAccount.name).get())
                .build();
    }

}
