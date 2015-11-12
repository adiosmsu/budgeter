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

import org.junit.Test;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Date: 11/10/15
 * Time: 4:11 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPriceElementCoreTest extends AbstractFundsTester {

    @Test
    public void testSubmit() throws Exception {
        innerTestSubmit();
    }

    @Override
    protected void actualTest(Bundle bundle,
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
                              FundsMutationSubject guitarSubj) {
        final OffsetDateTime now = OffsetDateTime.now();

        PriceAdditionElementCore core = new PriceAdditionElementCore(accounter.subjectPriceRepository(), accounter.fundsMutationSubjectRepo());
        core.setDay(new UtcDay());
        core.setAgent(groceryAgent);
        core.setSubject(breadSubj);
        core.setPriceDecimal(BigDecimal.valueOf(70));
        core.setPriceUnit(Units.RUB);
        Submitter.Result<SubjectPrice> result = core.submit();
        result.raiseExceptionIfFailed();

        assertTrue(accounter.subjectPriceRepository().priceExists(breadSubj, groceryAgent, new UtcDay()));

        try {
            core = new PriceAdditionElementCore(accounter.subjectPriceRepository(), accounter.fundsMutationSubjectRepo());
            core.setDay(new UtcDay());
            core.setAgent(inetAgent);
            core.setSubject(cardSubj);
            core.setPriceDecimal(BigDecimal.valueOf(70));
            result = core.submit();
            result.raiseExceptionIfFailed();
            fail();
        } catch (Exception ignore) {}
    }

}
