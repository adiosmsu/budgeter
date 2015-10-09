package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Date: 6/15/15
 * Time: 6:54 PM
 *
 * @author Mikhail Kulikov
 */
public class TreasuryPseudoTableTest {

    @Test
    public void testAddAmount() throws Exception {
        Schema.clearSchema();
        final Treasury.BalanceAccount account = TestUtils.prepareBalance(CurrencyUnit.USD);
        final CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        fail("Concurrent interruption fail");
                    }
                    TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), account.name);
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);
        final Optional<Money> amount = TreasuryPseudoTable.INSTANCE.amount(CurrencyUnit.USD);
        final BigDecimal value = amount.get().getAmount();
        assertEquals("Concurrent add amount does not match", BigDecimal.valueOf(10L).setScale(2, BigDecimal.ROUND_HALF_DOWN), value);

        final Treasury.BalanceAccount accountBtc = TestUtils.prepareBalance(Units.BTC);
        try {
            TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), accountBtc);
            fail("Illegal addition of USD to BTC account");
        } catch (Exception ignore) {}
    }

    @Test
    public void testStreamRegisteredCurrencies() throws Exception {
        Schema.clearSchema();
        try {
            TestUtils.prepareBalance(CurrencyUnit.USD);
        } catch (Exception ignore) {}
        try {
            TestUtils.prepareBalance(CurrencyUnit.USD);
            fail("USD already there");
        } catch (Exception ignore) {}
        TestUtils.prepareBalance(CurrencyUnit.EUR);
        final List<CurrencyUnit> collected = TreasuryPseudoTable.INSTANCE.streamRegisteredCurrencies().sorted(new Comparator<CurrencyUnit>() {
            @Override
            public int compare(CurrencyUnit o1, CurrencyUnit o2) {
                final int sub = o1.getNumericCode() - o2.getNumericCode();
                return sub > 0 ? 1 : (sub == 0 ? 0 : -1);
            }
        }).collect(Collectors.<CurrencyUnit>toList());
        assertEquals("Wrong currency", collected.get(0), CurrencyUnit.USD);
        assertEquals("Wrong currency", collected.get(1), CurrencyUnit.EUR);
    }

    @Test
    public void testStreamAccountsByCurrency() throws Exception {
        Schema.clearSchema();
        try {
            TestUtils.prepareBalance(CurrencyUnit.USD);
        } catch (Exception ignore) {}
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), "accountUSD");
        final Treasury.BalanceAccount account = TreasuryPseudoTable.INSTANCE.streamAccountsByCurrency(CurrencyUnit.USD).findFirst().get();
        assertEquals("Wrong searched", account.getBalance(), Money.of(CurrencyUnit.USD, BigDecimal.ONE));
    }

    @Test
    public void testAmount() throws Exception {
        Schema.clearSchema();
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount accountEur = TestUtils.prepareBalance(CurrencyUnit.EUR);
        final Treasury.BalanceAccount accountUsd2 = new Treasury.BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        Schema.TREASURY.registerBalanceAccount(accountUsd2);

        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(10)), accountUsd);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd2);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);

        final Optional<Money> amountUsd = TreasuryPseudoTable.INSTANCE.amount(CurrencyUnit.USD);
        assertTrue("no USD on account balance", amountUsd.isPresent());
        assertEquals("Wrong USD accounts total balance", Money.of(CurrencyUnit.USD, BigDecimal.valueOf(40)), amountUsd.get());
        final Optional<Money> amountEur = TreasuryPseudoTable.INSTANCE.amount(CurrencyUnit.EUR);
        assertTrue("no EUR on account balance", amountEur.isPresent());
        assertEquals("Wrong EUR accounts total balance", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(100)), amountEur.get());
        final Optional<Money> amountAud = TreasuryPseudoTable.INSTANCE.amount(CurrencyUnit.AUD);
        assertFalse("AUD amount somehow present", amountAud.isPresent());
    }

    @Test
    public void testAccountBalance() throws Exception {
        Schema.clearSchema();
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount accountEur = TestUtils.prepareBalance(CurrencyUnit.EUR);
        final Treasury.BalanceAccount accountUsd2 = new Treasury.BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        Schema.TREASURY.registerBalanceAccount(accountUsd2);

        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(10)), accountUsd);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd2);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);
        TreasuryPseudoTable.INSTANCE.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);

        final Optional<Money> balanceUsd1 = TreasuryPseudoTable.INSTANCE.accountBalance(accountUsd);
        final Optional<Money> balanceUsd2 = TreasuryPseudoTable.INSTANCE.accountBalance("accountUSD2");
        final Optional<Money> balanceEur = TreasuryPseudoTable.INSTANCE.accountBalance(accountEur);
        assertTrue("no account balance for " + accountUsd, balanceUsd1.isPresent());
        assertTrue("no account balance for accountUSD2", balanceUsd2.isPresent());
        assertTrue("no account balance for " + accountEur, balanceEur.isPresent());
        assertEquals("Wrong balance for account " + accountUsd, Money.of(CurrencyUnit.USD, BigDecimal.valueOf(25)), balanceUsd1.get());
        assertEquals("Wrong balance for account accountUSD2", Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), balanceUsd2.get());
        assertEquals("Wrong balance for account " + accountEur, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(100)), balanceEur.get());
        final Optional<Money> balanceAud = TreasuryPseudoTable.INSTANCE.accountBalance("accountAUD");
        assertFalse("AUD account balance somehow present", balanceAud.isPresent());
    }

    @Test
    public void testRegisterBalanceAccount() throws Exception {
        Schema.clearSchema();
        TestUtils.prepareBalance(CurrencyUnit.USD);
        try {
            TreasuryPseudoTable.INSTANCE.registerBalanceAccount(new Treasury.BalanceAccount("accountUSD", CurrencyUnit.USD));
            fail("Two accounts with the same name somehow registered");
        } catch (Exception ignore) {}
    }

    @Test
    public void testStreamRegisteredAccounts() throws Exception {
        Schema.clearSchema();
        final Treasury.BalanceAccount accountUsd = TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount accountEur = TestUtils.prepareBalance(CurrencyUnit.EUR);
        final Treasury.BalanceAccount accountUsd2 = new Treasury.BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        Schema.TREASURY.registerBalanceAccount(accountUsd2);

        final Boolean[] checker = new Boolean[3];
        assertTrue("Name match failed [usd,usd2,eur]: " + Arrays.toString(checker), TreasuryPseudoTable.INSTANCE.streamRegisteredAccounts().allMatch(new Predicate<Treasury.BalanceAccount>() {
            @Override
            public boolean test(Treasury.BalanceAccount account) {
                if (account.getUnit().equals(CurrencyUnit.USD)) {
                    if (account.name.equals("accountUSD")) {
                        checker[0] = true;
                        return true;
                    } else if (account.name.equals("accountUSD2")) {
                        checker[1] = true;
                        return true;
                    }
                } else if (account.getUnit().equals(CurrencyUnit.EUR)) {
                    if (account.name.equals("accountEUR")) {
                        checker[2] = true;
                        return true;
                    }
                }
                return false;
            }
        }));
        assertTrue("Currency match failed [usd,usd2,eur]", checker[0] && checker[1] && checker[2]);
    }

    @Test
    public void testGetAccountWithId() throws Exception {
        Schema.clearSchema();
        TreasuryPseudoTable.INSTANCE.idSequence.set(0);
        TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount accountUsd = new Treasury.BalanceAccount("accountUSD", CurrencyUnit.USD);
        final Treasury.BalanceAccount accountWithId = TreasuryPseudoTable.INSTANCE.getAccountWithId(accountUsd);
        assertEquals(new Treasury.BalanceAccount(1L, "accountUSD", Money.of(CurrencyUnit.USD, BigDecimal.ZERO)), accountWithId);
    }

    @Test
    public void testGetAccountForName() throws Exception {
        Schema.clearSchema();
        TreasuryPseudoTable.INSTANCE.idSequence.set(0);
        TestUtils.prepareBalance(CurrencyUnit.USD);
        final Optional<Treasury.BalanceAccount> accountWithId = TreasuryPseudoTable.INSTANCE.getAccountForName("accountUSD");
        assertEquals(new Treasury.BalanceAccount(1L, "accountUSD", Money.of(CurrencyUnit.USD, BigDecimal.ZERO)), accountWithId.get());
        final Optional<Treasury.BalanceAccount> gibberish = TreasuryPseudoTable.INSTANCE.getAccountForName("gibberish");
        assertFalse(gibberish.isPresent());
    }

}