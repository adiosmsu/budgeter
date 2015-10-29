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

    final SafeJdbcTemplateProvider jdbcTemplateProvider;

    private final CurrencyRatesJdbcRepository currencyRates;
    private final CurrencyExchangeEventJdbcRepository currencyExchangeEvents;
    private final FundsMutationAgentJdbcRepository fundsMutationAgents;
    private final FundsMutationSubjectJdbcRepository fundsMutationSubjects;
    private final FundsMutationEventJdbcRepository fundsMutationEvents;
    private final PostponedCurrencyExchangeEventJdbcRepository postponedCurrencyExchangeEvents;
    private final PostponedFundsMutationEventJdbcRepository postponedFundsMutationEvents;
    private final JdbcTreasury treasury;
    private final JdbcAccounter accounter;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    public SourcingBundle(DataSource dataSource) {
        jdbcTemplateProvider = new SafeJdbcTemplateProvider(dataSource);
        currencyRates = new CurrencyRatesJdbcRepository(jdbcTemplateProvider);
        currencyExchangeEvents = new CurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
        fundsMutationAgents = new FundsMutationAgentJdbcRepository(jdbcTemplateProvider);
        fundsMutationSubjects = new FundsMutationSubjectJdbcRepository(jdbcTemplateProvider);
        fundsMutationEvents = new FundsMutationEventJdbcRepository(jdbcTemplateProvider, fundsMutationSubjects);
        postponedCurrencyExchangeEvents = new PostponedCurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
        postponedFundsMutationEvents = new PostponedFundsMutationEventJdbcRepository(jdbcTemplateProvider, fundsMutationEvents);
        treasury = new JdbcTreasury(jdbcTemplateProvider);
        accounter = new JdbcAccounter(this);
    }

    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        currencyRates.setSqlDialect(sqlDialect);
        currencyExchangeEvents.setSqlDialect(sqlDialect);
        fundsMutationAgents.setSqlDialect(sqlDialect);
        fundsMutationSubjects.setSqlDialect(sqlDialect);
        treasury.setSqlDialect(sqlDialect);
        fundsMutationEvents.setSqlDialect(sqlDialect);
        postponedCurrencyExchangeEvents.setSqlDialect(sqlDialect);
        postponedFundsMutationEvents.setSqlDialect(sqlDialect);
        accounter.setSqlDialect(sqlDialect);
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
        return fundsMutationEvents;
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents() {
        return postponedCurrencyExchangeEvents;
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
    public Accounter accounter() {
        return accounter;
    }

    @Override
    public void clearSchema() {
        // TODO: impl
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
            Common.executeMultipleSql(jdbcTemplate, fundsMutationEvents.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, postponedCurrencyExchangeEvents.getCreateTableSql());
            Common.executeMultipleSql(jdbcTemplate, postponedFundsMutationEvents.getCreateTableSql());
        }
    }

    @Override
    public void clear(Repo repo) {
        // TODO: impl
    }

}
