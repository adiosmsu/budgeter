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
import java8.util.OptionalLong;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.PostponedMutationEvent;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 10:16 AM
 *
 * @author Mikhail Kulikov
 */
final class StoredPostponedFundsMutationEvent extends Stored<PostponedMutationEvent> {

    final FundsMutationDirection direction;

    StoredPostponedFundsMutationEvent(
            int id, FundsMutationEvent obj, FundsMutationDirection direction, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate, boolean relevant
    ) {
        super(id, new PostponedMutationEvent(OptionalLong.of(id), obj, conversionUnit, customRate, relevant));
        this.direction = direction;
    }

}

