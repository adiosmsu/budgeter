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

import com.google.common.collect.ImmutableSet;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;

/**
 * Date: 6/12/15
 * Time: 7:26 PM
 *
 * @author Mikhail Kulikov
 */
public interface Accounter {

    FundsMutationEventRepository fundsMutationEventRepository();

    SubjectPriceRepository subjectPriceRepository();

    CurrencyExchangeEventRepository currencyExchangeEventRepository();

    PostponedFundsMutationEventRepository postponedFundsMutationEventRepository();

    PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository();


    Stream<PostponingReasons> streamAllPostponingReasons();

    Stream<PostponingReasons> streamAllPostponingReasons(boolean compatMode);

    FundsMutationSubjectRepository fundsMutationSubjectRepo();

    FundsMutationAgentRepository fundsMutationAgentRepo();

    @Immutable
    final class PostponingReasons {

        public final UtcDay dayUtc;
        public final ImmutableSet<CurrencyUnit> sufferingUnits;

        public PostponingReasons(UtcDay dayUtc, ImmutableSet<CurrencyUnit> sufferingUnits) {
            this.dayUtc = dayUtc;
            this.sufferingUnits = sufferingUnits;
        }

    }

}
