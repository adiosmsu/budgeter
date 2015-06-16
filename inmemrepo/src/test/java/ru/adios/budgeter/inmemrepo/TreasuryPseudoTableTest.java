package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 6/15/15
 * Time: 6:54 PM
 *
 * @author Mikhail Kulikov
 */
public class TreasuryPseudoTableTest {

    @Test
    public void testAddAmount() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    fail("Concurrent interruption fail");
                }
                TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE));
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);
        final Optional<Money> amount = TreasuryPseudoTable.INSTANCE.amount(CurrencyUnit.USD);
        final BigDecimal value = amount.get().getAmount();
        assertEquals("Concurrent add amount does not match", BigDecimal.valueOf(10L).setScale(2, BigDecimal.ROUND_HALF_DOWN), value);
    }

    @Test
    public void testGetRegisteredCurrencies() throws Exception {
        try {
            TreasuryPseudoTable.INSTANCE.registerCurrency(CurrencyUnit.USD);
        } catch (Exception ignore) {}
        try {
            TreasuryPseudoTable.INSTANCE.registerCurrency(CurrencyUnit.USD);
            fail("USD already there");
        } catch (Exception ignore) {}
        TreasuryPseudoTable.INSTANCE.registerCurrency(CurrencyUnit.EUR);
        final List<CurrencyUnit> collected = TreasuryPseudoTable.INSTANCE.getRegisteredCurrencies().sorted((o1, o2) -> {
            final int sub = o1.getNumericCode() - o2.getNumericCode();
            return sub > 0 ? 1 : (sub == 0 ? 0 : -1);
        }).collect(Collectors.toList());
        assertEquals("Wrong currency", collected.get(0), CurrencyUnit.USD);
        assertEquals("Wrong currency", collected.get(1), CurrencyUnit.EUR);
    }

    @Test
    public void testSearchCurrenciesByString() throws Exception {
        try {
            TreasuryPseudoTable.INSTANCE.registerCurrency(CurrencyUnit.USD);
        } catch (Exception ignore) {}
        assertEquals("Wrong searched", TreasuryPseudoTable.INSTANCE.searchCurrenciesByString("US").get(0), CurrencyUnit.USD);
    }

    @Test
    public void testTotalAmount() throws Exception {
        TreasuryPseudoTable.INSTANCE.clear();
    }

}