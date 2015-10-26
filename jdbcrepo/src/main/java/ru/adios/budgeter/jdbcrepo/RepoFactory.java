package ru.adios.budgeter.jdbcrepo;

import ru.adios.budgeter.api.*;

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

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;

    public RepoFactory(DataSource dataSource) {
        jdbcTemplateProvider = new SafeJdbcTemplateProvider(dataSource);
    }

    public void setNewDataSource(DataSource dataSource) {
        jdbcTemplateProvider.setDataSource(dataSource);
    }

    public FundsMutationSubjectRepository createFundsMutationSubjects() {
        return null;
    }

    public CurrencyExchangeEventJdbcRepository createCurrencyExchangeEvents() {
        return new CurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
    }

    public FundsMutationEventRepository createFundsMutationEvents() {
        return null;
    }

    public PostponedCurrencyExchangeEventRepository createPostponedCurrencyExchangeEvents() {
        return null;
    }

    public PostponedFundsMutationEventRepository createPostponedFundsMutationEvents() {
        return null;
    }

    public Treasury createTreasury() {
        return null;
    }

    public CurrencyRatesRepository createCurrencyRates() {
        return null;
    }

    public FundsMutationAgentJdbcRepository createFundsMutationAgents() {
        return new FundsMutationAgentJdbcRepository(jdbcTemplateProvider);
    }

}
