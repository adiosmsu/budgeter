package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableMap;
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
    private final ImmutableMap<Repo, JdbcRepository> order;
    private final JdbcAccounter accounter;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    public SourcingBundle(DataSource dataSource) {
        jdbcTemplateProvider = new SafeJdbcTemplateProvider(dataSource);

        final CurrencyRatesJdbcRepository currencyRates = new CurrencyRatesJdbcRepository(jdbcTemplateProvider);
        final CurrencyExchangeEventJdbcRepository currencyExchangeEvents = new CurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
        final FundsMutationAgentJdbcRepository fundsMutationAgents = new FundsMutationAgentJdbcRepository(jdbcTemplateProvider);
        final FundsMutationSubjectJdbcRepository fundsMutationSubjects = new FundsMutationSubjectJdbcRepository(jdbcTemplateProvider);
        final FundsMutationEventJdbcRepository fundsMutationEvents = new FundsMutationEventJdbcRepository(jdbcTemplateProvider, fundsMutationSubjects);
        final PostponedCurrencyExchangeEventJdbcRepository postponedCurrencyExchangeEvents = new PostponedCurrencyExchangeEventJdbcRepository(jdbcTemplateProvider);
        final PostponedFundsMutationEventJdbcRepository postponedFundsMutationEvents = new PostponedFundsMutationEventJdbcRepository(jdbcTemplateProvider, fundsMutationEvents);
        final JdbcTreasury treasury = new JdbcTreasury(jdbcTemplateProvider);

        accounter = new JdbcAccounter(this);

        order = ImmutableMap.<Repo, JdbcRepository>builder()
                .put(Repo.TREASURY, treasury)
                .put(Repo.FUNDS_MUTATION_SUBJECTS, fundsMutationSubjects)
                .put(Repo.FUNDS_MUTATION_AGENTS, fundsMutationAgents)
                .put(Repo.CURRENCY_RATES, currencyRates)
                .put(Repo.CURRENCY_EXCHANGE_EVENTS, currencyExchangeEvents)
                .put(Repo.FUNDS_MUTATION_EVENTS, fundsMutationEvents)
                .put(Repo.POSTPONED_CURRENCY_EXCHANGE_EVENTS, postponedCurrencyExchangeEvents)
                .put(Repo.POSTPONED_FUNDS_MUTATION_EVENTS, postponedFundsMutationEvents)
                .build();
    }

    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        for (final JdbcRepository repository : order.values()) {
            repository.setSqlDialect(sqlDialect);
        }
        accounter.setSqlDialect(sqlDialect);
    }

    public void setNewDataSource(DataSource dataSource) {
        jdbcTemplateProvider.setDataSource(dataSource);
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjects() {
        return (FundsMutationSubjectRepository) order.get(Repo.FUNDS_MUTATION_SUBJECTS);
    }

    @Override
    public CurrencyExchangeEventJdbcRepository currencyExchangeEvents() {
        return (CurrencyExchangeEventJdbcRepository) order.get(Repo.CURRENCY_EXCHANGE_EVENTS);
    }

    @Override
    public FundsMutationEventRepository fundsMutationEvents() {
        return (FundsMutationEventRepository) order.get(Repo.FUNDS_MUTATION_EVENTS);
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEvents() {
        return (PostponedCurrencyExchangeEventRepository) order.get(Repo.POSTPONED_CURRENCY_EXCHANGE_EVENTS);
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEvents() {
        return (PostponedFundsMutationEventRepository) order.get(Repo.POSTPONED_FUNDS_MUTATION_EVENTS);
    }

    @Override
    public Treasury treasury() {
        return (Treasury) order.get(Repo.TREASURY);
    }

    @Override
    public CurrencyRatesRepository currencyRates() {
        return (CurrencyRatesRepository) order.get(Repo.CURRENCY_RATES);
    }

    @Override
    public FundsMutationAgentJdbcRepository fundsMutationAgents() {
        return (FundsMutationAgentJdbcRepository) order.get(Repo.FUNDS_MUTATION_AGENTS);
    }

    @Override
    public Accounter accounter() {
        return accounter;
    }

    @Override
    public void clearSchema() {
        final JdbcTemplate jdbcTemplate = jdbcTemplateProvider.get();
        for (final JdbcRepository repo : order.values().asList().reverse()) {
            Common.executeMultipleSql(jdbcTemplate, repo.getDropTableSql());
        }
        createSchema(jdbcTemplate);
    }

    @Override
    public void createSchemaIfNeeded() {
        final JdbcTemplate jdbcTemplate = jdbcTemplateProvider.get();
        if (jdbcTemplate.query(sqlDialect.tableExistsSql(FundsMutationAgentJdbcRepository.TABLE_NAME), Common.STRING_ROW_MAPPER).isEmpty()) {
            createSchema(jdbcTemplate);
        }
    }

    private void createSchema(JdbcTemplate jdbcTemplate) {
        for (final JdbcRepository repo : order.values()) {
            Common.executeMultipleSql(jdbcTemplate, repo.getCreateTableSql());
        }
    }

    @Override
    public void clear(Repo repo) {
        clearRepo(jdbcTemplateProvider.get(), order.get(repo));
    }

    private void clearRepo(JdbcTemplate jdbcTemplate, JdbcRepository repository) {
        Common.executeMultipleSql(jdbcTemplate, repository.getDropTableSql());
        Common.executeMultipleSql(jdbcTemplate, repository.getCreateTableSql());
    }

}
