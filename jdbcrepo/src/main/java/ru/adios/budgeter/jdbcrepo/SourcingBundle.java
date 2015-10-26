package ru.adios.budgeter.jdbcrepo;

import ru.adios.budgeter.api.*;

import javax.sql.DataSource;

/**
 * Date: 10/26/15
 * Time: 7:32 PM
 *
 * @author Mikhail Kulikov
 */
public final class SourcingBundle implements Bundle {

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;



    public SourcingBundle(DataSource dataSource) {
        jdbcTemplateProvider = new SafeJdbcTemplateProvider(dataSource);
    }

    public void setNewDataSource(DataSource dataSource) {
        jdbcTemplateProvider.setDataSource(dataSource);
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjects() {
        return null;
    }

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEvents() {
        return null;
    }

    @Override
    public FundsMutationEventRepository fundsMutationEvents() {
        return null;
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents() {
        return null;
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEvents() {
        return null;
    }

    @Override
    public Treasury treasury() {
        return null;
    }

    @Override
    public CurrencyRatesRepository currencyRates() {
        return null;
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgents() {
        return null;
    }

    @Override
    public void clearSchema() {

    }

    @Override
    public void clear(Repo repo) {

    }

}
