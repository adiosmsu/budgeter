package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.util.Optional;

/**
 * Date: 7/1/15
 * Time: 7:50 AM
 *
 * @author Mikhail Kulikov
 */
public class TestUtils {

    static FundsMutationAgent prepareTestAgent(Bundle bundle) {
        final Optional<FundsMutationAgent> opt = bundle.fundsMutationAgents().findByName("Test");
        if (opt.isPresent()) {
            return opt.get();
        }

        return bundle.fundsMutationAgents().addAgent(
                FundsMutationAgent.builder()
                        .setName("Test")
                        .build()
        );
    }

    static BalanceAccount prepareBalance(Bundle bundle, CurrencyUnit unit) {
        final String name = "account" + unit.getCode();
        final Optional<BalanceAccount> account = bundle.treasury().getAccountForName(name);
        return account.isPresent()
                ? account.get()
                : bundle.treasury().registerBalanceAccount(new BalanceAccount(name, unit, null));
    }

    static FundsMutationSubject getSubject(Bundle bundle, String name) {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        final Optional<FundsMutationSubject> foodOpt = subjectRepository.findByName(name);
        if (foodOpt.isPresent()) {
            return foodOpt.get();
        }

        return subjectRepository.addSubject(
                FundsMutationSubject.builder(subjectRepository).setName(name).setType(FundsMutationSubject.Type.PRODUCT).build()
        );
    }

}
