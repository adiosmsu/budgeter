package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.TreasuryTester;

/**
 * Date: 6/15/15
 * Time: 6:54 PM
 *
 * @author Mikhail Kulikov
 */
public class JdbcTreasuryTest {

    private final TreasuryTester tester = new TreasuryTester(TestContext.BUNDLE);

    @Test
    public void testAccountBalance() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testAccountBalance();
            }
        });
    }

    @Test
    public void testStreamRegisteredAccounts() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamRegisteredAccounts();
            }
        });
    }

    @Test
    public void testAddAmount() throws Exception {
        tester.testAddAmount();
    }

    @Test
    public void testGetAccountForName() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testGetAccountForName();
            }
        });
    }

    @Test
    public void testAmount() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testAmount();
            }
        });
    }

    @Test
    public void testRegisterBalanceAccount() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testRegisterBalanceAccount();
            }
        });
    }

    @Test
    public void testGetAccountWithId() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testGetAccountWithId();
            }
        });
    }

    @Test
    public void testStreamAccountsByCurrency() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamAccountsByCurrency();
            }
        });
    }

    @Test
    public void testStreamRegisteredCurrencies() throws Exception {
        TestContext.TRANSACTIONAL_SUPPORT.runWithTransaction(new TestCheckedRunnable() {
            @Override
            public void runChecked() throws Exception {
                tester.testStreamRegisteredCurrencies();
            }
        });
    }

}