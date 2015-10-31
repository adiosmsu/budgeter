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

}
