package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.BigDecimal;

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
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        bundle.clearSchema();
        final Treasury treasury = bundle.treasury();
        final BalanceAccount usdAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        final BalanceAccount eurAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);

        FundsAdditionElementCore core = new FundsAdditionElementCore(treasury);
        core.setAmount(100, 0);
        core.setAccount(usdAccount);
        Submitter.Result<BalanceAccount> submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<Money> amount = Schema.TREASURY.amount(CurrencyUnit.USD);
        assertTrue(amount.isPresent());
        assertEquals(caseName + "Treasury amount fault", Money.of(CurrencyUnit.USD, 100.0), amount.get());

        core = new FundsAdditionElementCore(treasury);
        core.setAmountUnit(CurrencyUnit.EUR);
        try {
            submit = core.submit();
            submit.raiseExceptionIfFailed();
            fail(caseName + "No exception though amount not set");
        } catch (Exception ignore) {}
        core = new FundsAdditionElementCore(treasury);
        core.setAmountDecimal(BigDecimal.valueOf(100.));
        try {
            submit = core.submit();
            submit.raiseExceptionIfFailed();
            fail(caseName + "No exception though unit not set");
        } catch (Exception ignore) {}

        core = new FundsAdditionElementCore(treasury);
        core.setAmount(Money.of(CurrencyUnit.EUR, 10.));
        core.setAccount(eurAccount);
        submit = core.submit();
        submit.raiseExceptionIfFailed();

        final Optional<Money> amountEur = Schema.TREASURY.amount(CurrencyUnit.EUR);
        assertTrue(amountEur.isPresent());
        assertEquals(caseName + "Treasury amount fault", Money.of(CurrencyUnit.EUR, 10.), amountEur.get());

        core = new FundsAdditionElementCore(treasury);
        core.setAmount(Money.of(CurrencyUnit.USD, -10.));
        core.setAccount(usdAccount);
        submit = core.submit();
        try {
            submit.raiseExceptionIfFailed();
            fail(caseName + "Negative amount passed");
        } catch (Submitter.SubmitFailure ignore) {}

        final Optional<Money> amountUsd = Schema.TREASURY.amount(CurrencyUnit.USD);
        assertTrue(amountUsd.isPresent());
        assertEquals(caseName + "Treasury amount fault", Money.of(CurrencyUnit.USD, 100.0), amountUsd.get());
    }

}