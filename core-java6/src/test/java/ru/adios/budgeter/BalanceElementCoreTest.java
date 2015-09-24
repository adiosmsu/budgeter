package ru.adios.budgeter;

import java8.util.function.Consumer;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.UtcDay;
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

    private final TreasuryMock treasury = new TreasuryMock();
    private final CurrencyRatesRepositoryMock ratesRepository = new CurrencyRatesRepositoryMock();
    private BalanceElementCore core = new BalanceElementCore(treasury, ratesRepository);

    @Test
    public void testSetTotalUnit() throws Exception {
        try {
            core.setTotalUnit(null);
            fail("Null accepted");
        } catch (Exception ignore) {}
        core.setTotalUnit(CurrencyUnit.USD);
    }

    @Test
    public void testStreamIndividualBalances() throws Exception {
        treasury.clear();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        core.streamIndividualBalances().forEach(new Consumer<Money>() {
            @Override
            public void accept(Money money) {
                assertEquals("Wrong balance for " + money,
                        money.getCurrencyUnit().equals(CurrencyUnit.USD) ? BigDecimal.valueOf(1000).stripTrailingZeros() : BigDecimal.valueOf(500).stripTrailingZeros(),
                        money.getAmount().stripTrailingZeros());
            }
        });
    }

    @Test
    public void testGetTotalBalance() throws Exception {
        treasury.clear();
        ratesRepository.clear();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
        core.setTotalUnit(Units.RUB);
        assertEquals("Wrong total balance", Money.of(Units.RUB, BigDecimal.valueOf(87500.)), core.getTotalBalance());

        // same total unit test
        Schema.clearSchema();
        treasury.registerBalanceAccount(new Treasury.BalanceAccount("Тест", Units.RUB));
        core.setTotalUnit(Units.RUB);
        core.getTotalBalance();
    }

    @Test
    public void testNoTodayRate() throws Exception {
        ratesRepository.clear();
        treasury.clear();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000), "usd");
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500), "eur");
        assertTrue("Miraculously rates present", core.noTodayRate());
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, Units.RUB, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, Units.RUB, BigDecimal.valueOf(65));
        core.setTotalUnit(Units.RUB);
        assertFalse("Rates don't present though we added them", core.noTodayRate());
    }

}