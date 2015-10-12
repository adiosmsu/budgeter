package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Date: 7/7/15
 * Time: 7:12 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsAdditionElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        Schema.clearSchema();
        final TreasuryMock treasury = new TreasuryMock();
        final Treasury.BalanceAccount usdAccount = TestUtils.prepareBalance(CurrencyUnit.USD);
        final Treasury.BalanceAccount eurAccount = TestUtils.prepareBalance(CurrencyUnit.EUR);

        FundsAdditionElementCore core = new FundsAdditionElementCore(treasury);
        core.setAmount(100, 0);
        core.setAccount(usdAccount);
        Submitter.Result<Treasury.BalanceAccount> submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<Money> amount = Schema.TREASURY.amount(CurrencyUnit.USD);
        assertTrue(amount.isPresent());
        assertEquals("Treasury amount fault", Money.of(CurrencyUnit.USD, 100.0), amount.get());

        core = new FundsAdditionElementCore(treasury);
        core.setAmountUnit(CurrencyUnit.EUR);
        try {
            submit = core.submit();
            submit.raiseExceptionIfFailed();
            fail("No exception though amount not set");
        } catch (Exception ignore) {}
        core = new FundsAdditionElementCore(treasury);
        core.setAmountDecimal(BigDecimal.valueOf(100.));
        try {
            submit = core.submit();
            submit.raiseExceptionIfFailed();
            fail("No exception though unit not set");
        } catch (Exception ignore) {}

        core = new FundsAdditionElementCore(treasury);
        core.setAmount(Money.of(CurrencyUnit.EUR, 10.));
        core.setAccount(eurAccount);
        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<Money> amountEur = Schema.TREASURY.amount(CurrencyUnit.EUR);
        assertTrue(amountEur.isPresent());
        assertEquals("Treasury amount fault", Money.of(CurrencyUnit.EUR, 10.), amountEur.get());

        core = new FundsAdditionElementCore(treasury);
        core.setAmount(Money.of(CurrencyUnit.USD, -10.));
        core.setAccount(usdAccount);
        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<Money> amountUsd = Schema.TREASURY.amount(CurrencyUnit.USD);
        assertTrue(amountUsd.isPresent());
        assertEquals("Treasury amount fault", Money.of(CurrencyUnit.USD, 90.0), amountUsd.get());
    }

}