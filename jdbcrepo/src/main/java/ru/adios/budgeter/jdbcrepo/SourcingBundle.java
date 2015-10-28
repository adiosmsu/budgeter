package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.adios.budgeter.api.*;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;

/**
 * Date: 10/26/15
 * Time: 7:32 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public final class SourcingBundle implements Bundle {

    private final SafeJdbcTemplateProvider jdbcTemplateProvider;

    private final CurrencyRatesJdbcRepository currencyRates;
    private final CurrencyExchangeEventJdbcRepository currencyExchangeEvents;
    private final FundsMutationAgentJdbcRepository fundsMutationAgents;
    private final FundsMutationSubjectJdbcRepository fundsMutationSubjects;
    private final JdbcTreasury treasury;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    public SourcingBundle(DataSource dataSource) {
        jdbcTemplateProvider = new SafeJdbcTemplateProvider(dataSource);
        currencyRates = new CurrencyRatesJdbcRepository(jdbcTemplateProvider);
        currencyExchangeEvents = new CurrencyExchangeEventJdbcRepository(this, jdbcTemplateProvider);
        fundsMutationAgents = new FundsMutationAgentJdbcRepository(jdbcTemplateProvider);
        fundsMutationSubjects = new FundsMutationSubjectJdbcRepository(jdbcTemplateProvider);
        treasury = new JdbcTreasury(jdbcTemplateProvider);
    }

    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        currencyRates.setSqlDialect(sqlDialect);
        currencyExchangeEvents.setSqlDialect(sqlDialect);
        fundsMutationAgents.setSqlDialect(sqlDialect);
        fundsMutationSubjects.setSqlDialect(sqlDialect);
        treasury.setSqlDialect(sqlDialect);
        //TODO: finish
    }

    public void setNewDataSource(DataSource dataSource) {
        jdbcTemplateProvider.setDataSource(dataSource);
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjects() {
        return fundsMutationSubjects;
    }

    @Override
    public CurrencyExchangeEventJdbcRepository currencyExchangeEvents() {
        return currencyExchangeEvents;
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
        return treasury;
    }

    @Override
    public CurrencyRatesRepository currencyRates() {
        return currencyRates;
    }

    @Override
    public FundsMutationAgentJdbcRepository fundsMutationAgents() {
        return fundsMutationAgents;
    }

    @Override
    public void clearSchema() {

    }

    @Override
    public void createSchemaIfNeeded() {
        final JdbcTemplate jdbcTemplate = jdbcTemplateProvider.get();
        if (jdbcTemplate.query(sqlDialect.tableExistsSql(FundsMutationAgentJdbcRepository.TABLE_NAME), Common.STRING_ROW_MAPPER).isEmpty()) {
            Common.executeMultipleSql(jdbcTemplate, currencyRates.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, currencyExchangeEvents.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, fundsMutationAgents.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, fundsMutationSubjects.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, treasury.getCreateTableSql());

            // TODO: finish
        }
    }

    @Override
    public void clear(Repo repo) {

    }

}
