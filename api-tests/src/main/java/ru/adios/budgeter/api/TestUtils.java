package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

/**
 * Date: 7/1/15
 * Time: 7:50 AM
 *
 * @author Mikhail Kulikov
 */
public class TestUtils {

    static FundsMutationAgent prepareTestAgent(Bundle bundle) {
        final FundsMutationAgent test = FundsMutationAgent.builder().setName("Test").build();
        bundle.clear(Bundle.Repo.FUNDS_MUTATION_AGENTS);
        bundle.fundsMutationAgents().addAgent(test);
        return test;
    }

    static Treasury.BalanceAccount prepareBalance(Bundle bundle, CurrencyUnit unit) {
        final Treasury.BalanceAccount account = new Treasury.BalanceAccount("account" + unit.getCode(), unit);
        bundle.treasury().registerBalanceAccount(account);
        return account;
    }

}
