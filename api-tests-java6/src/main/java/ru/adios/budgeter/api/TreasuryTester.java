package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.data.BalanceAccount;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Date: 10/26/15
 * Time: 6:38 PM
 *
 * @author Mikhail Kulikov
 */
public final class TreasuryTester {

    private static final Logger logger = LoggerFactory.getLogger(TreasuryTester.class);

    private final Bundle bundle;

    public TreasuryTester(Bundle bundle) {
        this.bundle = bundle;
    }


    public void testAddAmount() throws Exception {
        bundle.clearSchema();

        final Treasury treasury = bundle.treasury();

        final TransactionalSupport txs = bundle.getTransactionalSupport();
        final BalanceAccount account = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
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
                    if (txs != null) {
                        txs.runWithTransaction(new Runnable() {
                            @Override
                            public void run() {
                                logger.info("Fire transactional");
                                treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), account.name);
                            }
                        });
                    } else {
                        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), account.name);
                    }
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(1000);
        final Optional<Money> amount = treasury.amount(CurrencyUnit.USD);
        final BigDecimal value = amount.get().getAmount();
        assertEquals("Concurrent add amount does not match", BigDecimal.valueOf(10L).setScale(2, BigDecimal.ROUND_HALF_DOWN), value);

        if (txs != null) {
            txs.runWithTransaction(new Runnable() {
                @Override
                public void run() {
                    testAddAmountInner(treasury);
                }
            });
        } else {
            testAddAmountInner(treasury);
        }
    }

    private void testAddAmountInner(Treasury treasury) {
        final BalanceAccount accountBtc = TestUtils.prepareBalance(bundle, Units.BTC);
        try {
            treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), accountBtc);
            fail("Illegal addition of USD to BTC account");
        } catch (Exception ignore) {}
    }

    public void testStreamRegisteredCurrencies() throws Exception {
        bundle.clearSchema();

        final Treasury treasury = bundle.treasury();

        try {
            TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        } catch (Exception ignore) {}
        try {
            TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
            fail("USD already there");
        } catch (Exception ignore) {}
        TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final List<CurrencyUnit> collected = treasury.streamRegisteredCurrencies().sorted(new Comparator<CurrencyUnit>() {
            @Override
            public int compare(CurrencyUnit o1, CurrencyUnit o2) {
                final int sub = o1.getNumericCode() - o2.getNumericCode();
                return sub > 0 ? 1 : (sub == 0 ? 0 : -1);
            }
        }).collect(Collectors.<CurrencyUnit>toList());
        assertEquals("Wrong currency", collected.get(0), CurrencyUnit.USD);
        assertEquals("Wrong currency", collected.get(1), CurrencyUnit.EUR);
    }

    public void testStreamAccountsByCurrency() throws Exception {
        bundle.clearSchema();
        try {
            TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        } catch (Exception ignore) {}
        bundle.treasury().addAmount(Money.of(CurrencyUnit.USD, BigDecimal.ONE), "accountUSD");
        final BalanceAccount account = bundle.treasury().streamAccountsByCurrency(CurrencyUnit.USD).findFirst().get();
        assertEquals("Wrong searched", account.getBalance().get(), Money.of(CurrencyUnit.USD, BigDecimal.ONE));
    }

    public void testAmount() throws Exception {
        bundle.clearSchema();

        final Treasury treasury = bundle.treasury();

        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final BalanceAccount accountUsd2 = new BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        treasury.registerBalanceAccount(accountUsd2);

        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(10)), accountUsd);
        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd);
        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd2);
        treasury.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);
        treasury.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);

        final Optional<Money> amountUsd = treasury.amount(CurrencyUnit.USD);
        assertTrue("no USD on account balance", amountUsd.isPresent());
        assertEquals("Wrong USD accounts total balance", Money.of(CurrencyUnit.USD, BigDecimal.valueOf(40)), amountUsd.get());
        final Optional<Money> amountEur = treasury.amount(CurrencyUnit.EUR);
        assertTrue("no EUR on account balance", amountEur.isPresent());
        assertEquals("Wrong EUR accounts total balance", Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(100)), amountEur.get());
        final Optional<Money> amountAud = treasury.amount(CurrencyUnit.AUD);
        assertFalse("AUD amount somehow present", amountAud.isPresent());
    }

    public void testAccountBalance() throws Exception {
        bundle.clearSchema();

        final Treasury treasury = bundle.treasury();

        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final BalanceAccount accountUsd2 = new BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        treasury.registerBalanceAccount(accountUsd2);

        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(10)), accountUsd);
        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd);
        treasury.addAmount(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), accountUsd2);
        treasury.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);
        treasury.addAmount(Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(50)), accountEur);

        final Optional<Money> balanceUsd1 = treasury.accountBalance(accountUsd);
        final Optional<Money> balanceUsd2 = treasury.accountBalance("accountUSD2");
        final Optional<Money> balanceEur = treasury.accountBalance(accountEur);
        assertTrue("no account balance for " + accountUsd, balanceUsd1.isPresent());
        assertTrue("no account balance for accountUSD2", balanceUsd2.isPresent());
        assertTrue("no account balance for " + accountEur, balanceEur.isPresent());
        assertEquals("Wrong balance for account " + accountUsd, Money.of(CurrencyUnit.USD, BigDecimal.valueOf(25)), balanceUsd1.get());
        assertEquals("Wrong balance for account accountUSD2", Money.of(CurrencyUnit.USD, BigDecimal.valueOf(15)), balanceUsd2.get());
        assertEquals("Wrong balance for account " + accountEur, Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(100)), balanceEur.get());
        final Optional<Money> balanceAud = treasury.accountBalance("accountAUD");
        assertFalse("AUD account balance somehow present", balanceAud.isPresent());
    }

    public void testRegisterBalanceAccount() throws Exception {
        bundle.clearSchema();
        TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        try {
            bundle.treasury().registerBalanceAccount(new BalanceAccount("accountUSD", CurrencyUnit.USD));
            fail("Two accounts with the same name somehow registered");
        } catch (Exception ignore) {}
    }

    public void testStreamRegisteredAccounts() throws Exception {
        bundle.clearSchema();
        final BalanceAccount accountUsd = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount accountEur = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);
        final BalanceAccount accountUsd2 = new BalanceAccount("account" + CurrencyUnit.USD.getCode() + '2', CurrencyUnit.USD);
        bundle.treasury().registerBalanceAccount(accountUsd2);

        final Boolean[] checker = new Boolean[3];
        assertTrue("Name match failed [usd,usd2,eur]: " + Arrays.toString(checker), bundle.treasury().streamRegisteredAccounts().allMatch(new Predicate<BalanceAccount>() {
            @Override
            public boolean test(BalanceAccount account) {
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

    public void testGetAccountWithId() throws Exception {
        bundle.clearSchema();
        bundle.treasury().setSequenceValue(0L);
        TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount accountUsd = new BalanceAccount("accountUSD", CurrencyUnit.USD);
        final BalanceAccount accountWithId = bundle.treasury().getAccountWithId(accountUsd);
        assertEquals(new BalanceAccount(1L, "accountUSD", Money.of(CurrencyUnit.USD, BigDecimal.ZERO)), accountWithId);
    }

    public void testGetAccountForName() throws Exception {
        bundle.clearSchema();
        bundle.treasury().setSequenceValue(0L);
        TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final Optional<BalanceAccount> accountWithId = bundle.treasury().getAccountForName("accountUSD");
        assertEquals(new BalanceAccount(1L, "accountUSD", Money.of(CurrencyUnit.USD, BigDecimal.ZERO)), accountWithId.get());
        final Optional<BalanceAccount> gibberish = bundle.treasury().getAccountForName("gibberish");
        assertFalse(gibberish.isPresent());
    }

}
