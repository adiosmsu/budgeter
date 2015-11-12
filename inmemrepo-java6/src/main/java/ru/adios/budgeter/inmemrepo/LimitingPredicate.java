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
import java8.util.function.Predicate;
import ru.adios.budgeter.api.OptLimit;

/**
 * Date: 11/9/15
 * Time: 12:17 PM
 *
 * @author Mikhail Kulikov
 */
public class LimitingPredicate<T> implements Predicate<T> {

    private final Optional<OptLimit> limitRef;

    private int offsetCounter;
    private int limitCounter;

    public LimitingPredicate(Optional<OptLimit> limitRef) {
        this.limitRef = limitRef;
    }

    @Override
    public boolean test(T t) {
        if (!limitRef.isPresent()) {
            return true;
        }
        final OptLimit limit = limitRef.get();
        return !(limit.offset > 0 && limit.offset > offsetCounter++)
                && !(limit.limit > 0 && limit.limit < ++limitCounter);
    }

}
