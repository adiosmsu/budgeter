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

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 1:33 PM
 *
 * @author Mikhail Kulikov
 */
public interface PseudoTable<Row> {

    Row get(int id);

    void clear();

    static <Row, IndexedType> Row nonUniqueIndexedInsert(
            final ConcurrentHashMap<Integer, Row> table,
            final ConcurrentHashMap<IndexedType, ImmutableSet<Integer>> index,
            final int id,
            final Function<Integer, Row> insertionSupplier,
            final Optional<IndexedType> indexedValueRef,
            final Optional<Consumer<Row>> rowConstraint,
            final Consumer... uniqueConstraints
    ) {
        return table.computeIfAbsent(id, key -> {
            if (uniqueConstraints.length > 0) {
                for (Consumer uniqueConstraint : uniqueConstraints) {
                    //noinspection unchecked
                    uniqueConstraint.accept(id);
                }
            }

            if (indexedValueRef.isPresent()) {
                final IndexedType indexedValue = indexedValueRef.get();
                final long start = System.currentTimeMillis();
                ImmutableSet<Integer> stale;
                ImmutableSet<Integer> fresh;
                ImmutableSet<Integer> previous = null; // optimization to avoid redundant 'get' calls to concurrent map
                do { // atomic code regular trick; using atomic operations 'replace' and 'putIfAbsent' here
                    stale = index.get(indexedValue);
                    final ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
                    builder.add(key);
                    if (stale != null) {
                        // we are not the first one to use the index for indexedValue
                        if (rowConstraint.isPresent()) {
                            // row constraint is set, must check
                            for (Integer integer : stale) {
                                if (previous == null || !previous.contains(integer)) {
                                    // our first iteration so we must check every existing entry for that index value;
                                    // or not our first iteration hence we are in a race and must check all new entries supplied by our competitors
                                    Row toCheck;
                                    while ((toCheck = table.get(integer)) == null) { // sometimes in TIGHT races id in index might be visible earlier than table entry itself!
                                        checkState(System.currentTimeMillis() - start < 5000, "index update timeout: someone supplied table with null record; that's illegal");
                                    }
                                    rowConstraint.get().accept(toCheck);
                                }
                                builder.add(integer);
                            }
                            previous = stale;
                        } else {
                            builder.addAll(stale);
                        }
                    }
                    fresh = builder.build();
                    checkState(System.currentTimeMillis() - start < 5000, "index update timeout"); // five seconds is insane, drop out of the race!
                }
                while (
                        stale != null
                                ? !index.replace(indexedValue, stale, fresh)
                                : index.putIfAbsent(indexedValue, fresh) != null
                        );
            }

            return checkNotNull(insertionSupplier.apply(id));
        });
    }

}
