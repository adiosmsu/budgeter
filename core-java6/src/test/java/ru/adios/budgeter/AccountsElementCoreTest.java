package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
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
        Schema.clearSchema();

        AccountsElementCore accountsElementCore = new AccountsElementCore(Schema.TREASURY);
        try {
            accountsElementCore.setName(null);
            fail("Null values not allowed");
        } catch (NullPointerException ignore) {}
        accountsElementCore.setName("Ha-ha");
        accountsElementCore.setUnit(Units.RUB);
        Submitter.Result submit = accountsElementCore.submit();
        assertEquals("No submit success of ha-ha", Submitter.Result.SUCCESS, submit);

        final Optional<Money> haBalance = Schema.TREASURY.accountBalance("Ha-ha");
        assertTrue("No account for ha-ha", haBalance.isPresent());
        assertEquals("Money not zero for ha-ha", Money.zero(Units.RUB), haBalance.get());

        accountsElementCore = new AccountsElementCore(Schema.TREASURY);
        accountsElementCore.setName("Ho-ho");
        submit = accountsElementCore.submit();
        assertFalse("Submit unrightfully successful for ho-ho", submit.isSuccessful());
        assertEquals("Field error not unit", AccountsElementCore.FIELD_UNIT, submit.fieldErrors.get(0).fieldInFault);

        accountsElementCore = new AccountsElementCore(Schema.TREASURY);
        accountsElementCore.setUnit(CurrencyUnit.USD);
        submit = accountsElementCore.submit();
        assertFalse("Submit unrightfully successful for no-name", submit.isSuccessful());
        assertEquals("Field error not name", AccountsElementCore.FIELD_NAME, submit.fieldErrors.get(0).fieldInFault);
    }

}