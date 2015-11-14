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

import java8.util.Optional;
import java8.util.OptionalLong;
import org.threeten.bp.OffsetDateTime;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 11/3/15
 * Time: 11:01 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class PostponedExchange {

    public final OptionalLong id;
    public final BigDecimal toBuy;
    public final BalanceAccount toBuyAccount;
    public final BalanceAccount sellAccount;
    public final Optional<BigDecimal> customRate;
    public final OffsetDateTime timestamp;
    public final FundsMutationAgent agent;
    public final boolean relevant;

    public PostponedExchange(
            OptionalLong id, BigDecimal toBuy,
            BalanceAccount toBuyAccount,
            BalanceAccount sellAccount,
            Optional<BigDecimal> customRate,
            OffsetDateTime timestamp,
            FundsMutationAgent agent,
            boolean relevant) {
        this.id = id;
        this.agent = agent;
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.<BigDecimal>empty();
        this.toBuy = toBuy;
        this.toBuyAccount = toBuyAccount;
        this.sellAccount = sellAccount;
        this.timestamp = timestamp;
        this.relevant = relevant;
    }

}
