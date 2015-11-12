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

package ru.adios.budgeter.jdbcrepo;

import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;

/**
 * Date: 10/26/15
 * Time: 8:05 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public final class RepoFactory {

    private final SafeJdbcConnector jdbcConnector;

    public RepoFactory(DataSource dataSource) {
        jdbcConnector = new SafeJdbcConnector(dataSource);
    }

    public RepoFactory(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        jdbcConnector = new SafeJdbcConnector(dataSource, txSupport);
    }


    public void setNewDataSource(DataSource dataSource) {
        jdbcConnector.setDataSource(dataSource, null);
    }

    public void setNewDataSource(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        jdbcConnector.setDataSource(dataSource, txSupport);
    }


    public FundsMutationSubjectRepository createFundsMutationSubjects() {
        return new FundsMutationSubjectJdbcRepository(jdbcConnector);
    }

    public CurrencyExchangeEventJdbcRepository createCurrencyExchangeEvents() {
        return new CurrencyExchangeEventJdbcRepository(jdbcConnector);
    }

    public FundsMutationEventRepository createFundsMutationEvents() {
        return new FundsMutationEventJdbcRepository(jdbcConnector);
    }

    public PostponedCurrencyExchangeEventRepository createPostponedCurrencyExchangeEvents() {
        return new PostponedCurrencyExchangeEventJdbcRepository(jdbcConnector);
    }

    public PostponedFundsMutationEventRepository createPostponedFundsMutationEvents() {
        return new PostponedFundsMutationEventJdbcRepository(jdbcConnector);
    }

    public Treasury createTreasury() {
        return new JdbcTreasury(jdbcConnector);
    }

    public CurrencyRatesRepository createCurrencyRates() {
        return new CurrencyRatesJdbcRepository(jdbcConnector);
    }

    public FundsMutationAgentJdbcRepository createFundsMutationAgents() {
        return new FundsMutationAgentJdbcRepository(jdbcConnector);
    }

    public SubjectPriceRepository createSubjectPrices() {
        return new SubjectPriceJdbcRepository(jdbcConnector);
    }

}
