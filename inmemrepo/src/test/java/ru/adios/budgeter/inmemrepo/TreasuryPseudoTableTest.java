package ru.adios.budgeter.inmemrepo;

import org.junit.Test;
import ru.adios.budgeter.api.TreasuryTester;

/**
 * Date: 6/15/15
 * Time: 6:54 PM
 *
 * @author Mikhail Kulikov
 */
public class TreasuryPseudoTableTest {

    private final TreasuryTester tester = new TreasuryTester(Schema.INSTANCE);

    @Test
    public void testAccountBalance() throws Exception {
        tester.testAccountBalance();
    }

    @Test
    public void testStreamRegisteredAccounts() throws Exception {
        tester.testStreamRegisteredAccounts();
    }

    @Test
    public void testAddAmount() throws Exception {
        tester.testAddAmount();
    }

    @Test
    public void testGetAccountForName() throws Exception {
        tester.testGetAccountForName();
    }

    @Test
    public void testAmount() throws Exception {
        tester.testAmount();
    }

    @Test
    public void testRegisterBalanceAccount() throws Exception {
        tester.testRegisterBalanceAccount();
    }

    @Test
    public void testGetAccountWithId() throws Exception {
        tester.testGetAccountWithId();
    }

    @Test
    public void testStreamAccountsByCurrency() throws Exception {
        tester.testStreamAccountsByCurrency();
    }

    @Test
    public void testStreamRegisteredCurrencies() throws Exception {
        tester.testStreamRegisteredCurrencies();
    }

}