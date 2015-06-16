package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.UtcDay;

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
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000));
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500));
        core.streamIndividualBalances().forEach(money -> assertEquals("Wrong balance for " + money,
                money.getCurrencyUnit().equals(CurrencyUnit.USD) ? BigDecimal.valueOf(1000).stripTrailingZeros() : BigDecimal.valueOf(500).stripTrailingZeros(),
                money.getAmount().stripTrailingZeros()));
    }

    @Test
    public void testGetTotalBalance() throws Exception {
        treasury.clear();
        ratesRepository.clear();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000));
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500));
        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, rub, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, rub, BigDecimal.valueOf(65));
        core.setTotalUnit(rub);
        assertEquals("Wrong total balance", Money.of(rub, BigDecimal.valueOf(87500.)), core.getTotalBalance());
    }

    @Test
    public void testNoTodayRate() throws Exception {
        ratesRepository.clear();
        treasury.clear();
        treasury.addAmount(Money.of(CurrencyUnit.USD, 1000));
        treasury.addAmount(Money.of(CurrencyUnit.EUR, 500));
        final CurrencyUnit rub = CurrencyUnit.of("RUB");
        assertTrue("Miraculously rates present", core.noTodayRate());
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.USD, rub, BigDecimal.valueOf(55));
        ratesRepository.addRate(new UtcDay(), CurrencyUnit.EUR, rub, BigDecimal.valueOf(65));
        core.setTotalUnit(rub);
        assertFalse("Rates don't present though we added them", core.noTodayRate());
    }

}