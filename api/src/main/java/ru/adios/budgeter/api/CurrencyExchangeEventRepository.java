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

import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:52 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyExchangeEventRepository extends Provider<CurrencyExchangeEvent, Long> {

    enum Field implements OrderedField, Serializable {
        TIMESTAMP
    }

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

    int countExchangeEvents();

    Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<CurrencyExchangeEvent> streamForDay(UtcDay day);

    default Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamExchangeEvents(pair.options, pair.limit);
    }

}
