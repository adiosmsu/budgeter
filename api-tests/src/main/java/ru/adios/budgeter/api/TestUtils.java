/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

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
