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

package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 1:15 AM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public final class BalanceElementCore {

    private final Treasury treasury;
    private final CurrencyRatesProvider provider;

    private Optional<CurrencyUnit> totalUnitRef = Optional.empty();

    public BalanceElementCore(Treasury treasury, CurrencyRatesProvider provider) {
        this.treasury = treasury;
        this.provider = provider;
    }

    public void setTotalUnit(CurrencyUnit totalUnit) {
        this.totalUnitRef = Optional.ofNullable(totalUnit);
    }

    @Nullable
    public CurrencyUnit getTotalUnit() {
        return totalUnitRef.orElse(null);
    }

    @PotentiallyBlocking
    public Stream<Money> streamIndividualBalances() {
        return treasury.streamRegisteredCurrencies().map(unit -> {
            final Optional<Money> amount = treasury.amount(unit);
            return amount.orElse(Money.of(unit, SPECIAL));
        }).filter(money -> !money.getAmount().equals(SPECIAL));
    }

    @PotentiallyBlocking
    public Money getTotalBalance() {
        return treasury.totalAmount(totalUnitNonNull(), provider);
    }

    @PotentiallyBlocking
    public boolean noTodayRate() {
        final UtcDay today = new UtcDay();
        final CurrencyUnit main = totalUnitNonNull();
        final AtomicBoolean wasSomething = new AtomicBoolean(false);
        final boolean foundNoRateCase = treasury.streamRegisteredCurrencies().filter(unit -> {
            wasSomething.set(true);
            return !main.equals(unit) && !provider.getConversionMultiplier(today, main, unit).isPresent();
        }).findFirst().isPresent();
        return !wasSomething.get() || foundNoRateCase;
    }

    private CurrencyUnit totalUnitNonNull() {
        return totalUnitRef.orElse(CurrencyUnit.USD);
    }

    private static final BigDecimal SPECIAL = BigDecimal.valueOf(Long.MIN_VALUE);

}
