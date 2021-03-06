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

import java8.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

/**
 * Date: 6/15/15
 * Time: 1:21 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class AbstractPseudoTable<T extends Stored<U>, U> implements PseudoTable<T> {

    @Nonnull
    abstract ConcurrentHashMap<Integer, T> innerTable();

    @Override
    public final T get(int id) {
        return innerTable().get(id);
    }

    @Override
    public void clear() {
        innerTable().clear();
    }

}
