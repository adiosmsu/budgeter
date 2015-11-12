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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 10/24/15
 * Time: 7:43 PM
 *
 * @author Mikhail Kulikov
 */
public final class RepoUtil {

    public static final class Pair<T extends OrderedField> {
        final Optional<OptLimit> limit;
        @Nonnull final List<OrderBy<T>> options;

        private Pair(@Nullable OptLimit limit, @Nonnull List<OrderBy<T>> options) {
            this.limit = Optional.ofNullable(limit);
            this.options = options;
        }
    }

    public static <T extends OrderedField> Pair<T> parseOptVarArg(RepoOption[] options, Class<T> clazz) {
        final ArrayList<OrderBy<T>> repoOptions = new ArrayList<OrderBy<T>>(options.length);
        OptLimit limit = null;

        for (final RepoOption repoOption : options) {
            if (repoOption instanceof OptLimit) {
                limit = (OptLimit) repoOption;
            } else if (repoOption instanceof OrderBy) {
                final OrderBy orderBy = (OrderBy) repoOption;
                if (clazz.isAssignableFrom(orderBy.field.getClass())) {
                    //noinspection unchecked
                    repoOptions.add((OrderBy<T>) repoOption);
                }
            }
        }

        return new Pair<T>(limit, repoOptions);
    }

}
