package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;

import static org.junit.Assert.*;

/**
 * Date: 10/8/15
 * Time: 9:26 PM
 *
 * @author Mikhail Kulikov
 */
public class AccountsElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) {
        caseName += ": ";
        bundle.clearSchema();

        AccountsElementCore accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setName(null);
        accountsElementCore.setName("Ha-ha");
        accountsElementCore.setUnit(Units.RUB);
        Submitter.Result<Treasury.BalanceAccount> submit = accountsElementCore.submit();
        assertTrue(caseName + "No submit success of ha-ha", submit.isSuccessful());

        final Optional<Money> haBalance = bundle.treasury().accountBalance("Ha-ha");
        assertTrue(caseName + "No account for ha-ha", haBalance.isPresent());
        assertEquals(caseName + "Money not zero for ha-ha", Money.zero(Units.RUB), haBalance.get());

        accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setName("Ho-ho");
        submit = accountsElementCore.submit();
        assertFalse(caseName + "Submit unrightfully successful for ho-ho", submit.isSuccessful());
        assertEquals(caseName + "Field error not unit", AccountsElementCore.FIELD_UNIT, submit.fieldErrors.get(0).fieldInFault);

        accountsElementCore = new AccountsElementCore(bundle.treasury());
        accountsElementCore.setUnit(CurrencyUnit.USD);
        submit = accountsElementCore.submit();
        assertFalse(caseName + "Submit unrightfully successful for no-name", submit.isSuccessful());
        assertEquals(caseName + "Field error not name", AccountsElementCore.FIELD_NAME, submit.fieldErrors.get(0).fieldInFault);
    }

}