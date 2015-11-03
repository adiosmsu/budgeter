package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;

import java.util.Optional;

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
        return bundle.fundsMutationAgents().addAgent(test);
    }

    static BalanceAccount prepareBalance(Bundle bundle, CurrencyUnit unit) {
        final String name = "account" + unit.getCode();
        final Optional<BalanceAccount> account = bundle.treasury().getAccountForName(name);
        return account.isPresent()
                ? account.get()
                : bundle.treasury().registerBalanceAccount(new BalanceAccount(name, unit));
    }

}
