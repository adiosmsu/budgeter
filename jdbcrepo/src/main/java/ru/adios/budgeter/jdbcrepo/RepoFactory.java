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
        return new FundsMutationSubjectJdbcRepository(jdbcTemplateProvider);
    }

    public CurrencyExchangeEventJdbcRepository createCurrencyExchangeEvents() {
        return new CurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
    }

    public FundsMutationEventRepository createFundsMutationEvents() {
        return new FundsMutationEventJdbcRepository(jdbcTemplateProvider);
    }

    public PostponedCurrencyExchangeEventRepository createPostponedCurrencyExchangeEvents() {
        return new PostponedCurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
    }

    public PostponedFundsMutationEventRepository createPostponedFundsMutationEvents() {
        return new PostponedFundsMutationEventJdbcRepository(jdbcTemplateProvider);
    }

    public Treasury createTreasury() {
        return new JdbcTreasury(jdbcTemplateProvider);
    }

    public CurrencyRatesRepository createCurrencyRates() {
        return new CurrencyRatesJdbcRepository(jdbcTemplateProvider);
    }

    public FundsMutationAgentJdbcRepository createFundsMutationAgents() {
        return new FundsMutationAgentJdbcRepository(jdbcTemplateProvider);
    }

}
