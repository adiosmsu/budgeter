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

package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.inmemrepo.Schema;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Date: 11/10/15
 * Time: 4:02 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class AbstractFundsTester {

    protected final void innerTestSubmit() throws Exception {
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        final MathContext mc = new MathContext(7, RoundingMode.HALF_DOWN);
        final Accounter accounter = bundle.accounter();
        final Treasury treasury = bundle.treasury();

        final CurrenciesExchangeService ratesService = new CurrenciesExchangeService(
                bundle.getTransactionalSupport(),
                bundle.currencyRates(),
                accounter,
                treasury,
                ExchangeRatesLoader.createBtcLoader(treasury),
                ExchangeRatesLoader.createCbrLoader(treasury)
        );

        bundle.clearSchema();

        BalanceAccount rubAccount = TestUtils.prepareBalance(bundle, Units.RUB);
        BalanceAccount usdAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.USD);
        BalanceAccount eurAccount = TestUtils.prepareBalance(bundle, CurrencyUnit.EUR);

        final FundsMutationAgent groceryAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Магазин").build());
        final FundsMutationAgent inetAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Интернет").build());
        final FundsMutationAgent musicShopAgent = bundle.fundsMutationAgents().addAgent(FundsMutationAgent.builder().setName("Music shop").build());
        final FundsMutationSubject breadSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                        .setName("Хлеб")
                        .setType(FundsMutationSubject.Type.PRODUCT)
                        .build()
        );
        final FundsMutationSubject workSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                        .setName("Час работы")
                        .setType(FundsMutationSubject.Type.SERVICE)
                        .build()
        );
        final FundsMutationSubject cardSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                        .setName("NVidea 770GTX")
                        .setType(FundsMutationSubject.Type.PRODUCT)
                        .build()
        );
        final FundsMutationSubject guitarSubj = bundle.fundsMutationSubjects().addSubject(
                FundsMutationSubject.builder(accounter.fundsMutationSubjectRepo())
                        .setName("Gibson Les Paul")
                        .setType(FundsMutationSubject.Type.PRODUCT)
                        .build()
        );
        actualTest(
                bundle, caseName, mc, accounter, treasury, ratesService, rubAccount, groceryAgent, inetAgent, musicShopAgent, breadSubj, workSubj, cardSubj, guitarSubj
        );
    }

    protected abstract void actualTest(Bundle bundle,
                            String caseName,
                            MathContext mc,
                            Accounter accounter,
                            Treasury treasury,
                            CurrenciesExchangeService ratesService,
                            BalanceAccount rubAccount,
                            FundsMutationAgent groceryAgent,
                            FundsMutationAgent inetAgent,
                            FundsMutationAgent musicShopAgent,
                            FundsMutationSubject breadSubj,
                            FundsMutationSubject workSubj,
                            FundsMutationSubject cardSubj,
                            FundsMutationSubject guitarSubj);

}
