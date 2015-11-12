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
import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Date: 6/16/15
 * Time: 9:53 PM
 *
 * @author Mikhail Kulikov
 */
public class BalanceElementCoreTest {

    @Test
    public void testSetTotalUnit() throws Exception {
        testSetTotalUnitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSetTotalUnitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    public void testSetTotalUnitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        BalanceElementCore core = new BalanceElementCore(bundle.treasury(), bundle.currencyRates());
        core.setTotalUnit(null);
        assertNull(caseName + "set total unit null failed", core.getTotalUnit());
        core.setTotalUnit(CurrencyUnit.USD);
        assertEquals(caseName + "set USD unit null failed", CurrencyUnit.USD, core.getTotalUnit());
    }

    @Test
    public void testStreamIndividualBalances() throws Exception {
        testStreamIndividualBalancesWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testStreamIndividualBalancesWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    public void testStreamIndividualBalancesWith(Bundle bundle, String caseName) throws Exception {
        final String cn = caseName + ": ";
        final Treasury treasury = bundle.treasury();
        BalanceElementCore core = new BalanceElementCore(treasury, bundle.currencyRates());
        bundle.clearSchema();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        core.streamIndividualBalances().forEach(money -> assertEquals(cn + "Wrong balance for " + money,
                money.getCurrencyUnit().equals(CurrencyUnit.USD) ? BigDecimal.valueOf(1000).stripTrailingZeros() : BigDecimal.valueOf(500).stripTrailingZeros(),
                money.getAmount().stripTrailingZeros()));
    }

    @Test
    public void testGetTotalBalance() throws Exception {
        testGetTotalBalanceWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testGetTotalBalanceWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    public void testGetTotalBalanceWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        bundle.clearSchema();
        final Treasury treasury = bundle.treasury();
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();
        BalanceElementCore core = new BalanceElementCore(treasury, ratesRepository);
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
        core.setTotalUnit(Units.RUB);
        assertEquals(caseName + "Wrong total balance", Money.of(Units.RUB, BigDecimal.valueOf(87500.)), core.getTotalBalance());

        // same total unit test
        Schema.clearSchemaStatic();
        treasury.registerBalanceAccount(new BalanceAccount("Тест", Units.RUB, null));
        core.setTotalUnit(Units.RUB);
        core.getTotalBalance();

        // no total unit set test
        bundle.clearSchema();
        treasury.registerBalanceAccount(new BalanceAccount("Тест", Units.RUB, null));
        final BalanceElementCore balanceElementCore = new BalanceElementCore(treasury, ratesRepository);
        try {
            balanceElementCore.getTotalBalance();
            fail(caseName + "No way to get exchange rate between RUB and USD but it worked anyway!");
        } catch (NoRateException ignore) {}
    }

    @Test
    public void testNoTodayRate() throws Exception {
        testNoTodayRateWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testNoTodayRateWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    public void testNoTodayRateWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        bundle.clearSchema();
        final Treasury treasury = bundle.treasury();
        final CurrencyRatesRepository ratesRepository = bundle.currencyRates();
        BalanceElementCore core = new BalanceElementCore(treasury, ratesRepository);
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        assertTrue(caseName + "Miraculously rates present", core.noTodayRate());
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
        core.setTotalUnit(Units.RUB);
        assertFalse(caseName + "Rates don't present though we added them", core.noTodayRate());
    }

}