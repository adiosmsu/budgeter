package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.Treasury;

/**
 * Date: 7/1/15
 * Time: 7:50 AM
 *
 * @author Mikhail Kulikov
 */
public class TestUtils {

    static FundsMutationAgent prepareTestAgent() {
        final FundsMutationAgent test = FundsMutationAgent.builder().setName("Test").build();
        Schema.FUNDS_MUTATION_AGENTS.clear();
        Schema.FUNDS_MUTATION_AGENTS.addAgent(test);
        return test;
    }

    static Treasury.BalanceAccount prepareBalance(CurrencyUnit unit) {
        final Treasury.BalanceAccount account = new Treasury.BalanceAccount("account" + unit.getCode(), unit);
        Schema.TREASURY.registerBalanceAccount(account);
        return account;
    }

}
