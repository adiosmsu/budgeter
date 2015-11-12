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
import ru.adios.budgeter.api.PostponedFundsMutationEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:16 AM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedFundsMutationEventPseudoTable
        extends AbstractPseudoTable<StoredPostponedFundsMutationEvent, PostponedMutationEvent>
        implements PostponedFundsMutationEventRepository
{

    public static final PostponedFundsMutationEventPseudoTable INSTANCE = new PostponedFundsMutationEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, StoredPostponedFundsMutationEvent> table = new ConcurrentHashMap<>(100, 0.75f, 4);

    private PostponedFundsMutationEventPseudoTable() {}

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public Optional<PostponedMutationEvent> getById(Long id) {
        final StoredPostponedFundsMutationEvent stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public void rememberPostponedExchangeableEvent(FundsMutationEvent mutationEvent, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
        final StoredPostponedFundsMutationEvent event = new StoredPostponedFundsMutationEvent(
                idSequence.incrementAndGet(),
                mutationEvent,
                mutationEvent.amount.isPositive() ? FundsMutationDirection.BENEFIT : FundsMutationDirection.LOSS,
                paidUnit,
                customRate
        );
        checkState(table.putIfAbsent(event.id, event) == null);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedBenefits(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamRemembered(day, oneOf, secondOf, FundsMutationDirection.BENEFIT);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedLosses(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamRemembered(day, oneOf, secondOf, FundsMutationDirection.LOSS);
    }

    @Override
    public Stream<PostponedMutationEvent> streamRememberedEvents(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return streamRemembered(day, oneOf, secondOf, null);
    }

    Stream<PostponedMutationEvent> streamAll() {
        return table.values().stream().map(storedPostponedFundsMutationEvent -> storedPostponedFundsMutationEvent.obj);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, StoredPostponedFundsMutationEvent> innerTable() {
        return table;
    }

    @Override
    public void clear() {
        table.clear();
    }

    private Stream<PostponedMutationEvent> streamRemembered(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf, @Nullable FundsMutationDirection direction) {
        return table.values().stream().filter(event -> {
            final CurrencyUnit amountUnit = event.obj.mutationEvent.amount.getCurrencyUnit();
            return (direction == null || event.direction == direction)
                    && day.equals(new UtcDay(event.obj.mutationEvent.timestamp))
                    && (amountUnit.equals(oneOf) || amountUnit.equals(secondOf))
                    && (event.obj.conversionUnit.equals(oneOf) || event.obj.conversionUnit.equals(secondOf));
        }).map(event -> event.obj);
    }

}
