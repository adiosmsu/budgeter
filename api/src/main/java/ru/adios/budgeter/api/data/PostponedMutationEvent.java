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

package ru.adios.budgeter.api.data;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Date: 11/3/15
 * Time: 10:57 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class PostponedMutationEvent {

    public final OptionalLong id;
    public final FundsMutationEvent mutationEvent;
    public final CurrencyUnit conversionUnit;
    public final Optional<BigDecimal> customRate;
    public final boolean relevant;

    public PostponedMutationEvent(OptionalLong id, FundsMutationEvent mutationEvent, CurrencyUnit conversionUnit, Optional<BigDecimal> customRate, boolean relevant) {
        this.id = id;
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.empty();
        this.mutationEvent = mutationEvent;
        this.conversionUnit = conversionUnit;
        this.relevant = relevant;
    }

}
