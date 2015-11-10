package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.adios.budgeter.api.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/26/15
 * Time: 7:32 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public final class SourcingBundle implements Bundle {

    private static final Logger logger = LoggerFactory.getLogger(SourcingBundle.class);
    public static final Random RANDOM = new Random(System.currentTimeMillis());


    private final SafeJdbcConnector jdbcConnector;
    private final ImmutableMap<Repo, JdbcRepository> order;
    private final JdbcAccounter accounter;

    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    public SourcingBundle(DataSource dataSource) {
        this(dataSource, null);
    }

    public SourcingBundle(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        jdbcConnector = new SafeJdbcConnector(dataSource, txSupport);

        final CurrencyRatesJdbcRepository currencyRates = new CurrencyRatesJdbcRepository(jdbcConnector);
        final CurrencyExchangeEventJdbcRepository currencyExchangeEvents = new CurrencyExchangeEventJdbcRepository(jdbcConnector);
        final FundsMutationAgentJdbcRepository fundsMutationAgents = new FundsMutationAgentJdbcRepository(jdbcConnector);
        final FundsMutationSubjectJdbcRepository fundsMutationSubjects = new FundsMutationSubjectJdbcRepository(jdbcConnector);
        final FundsMutationEventJdbcRepository fundsMutationEvents = new FundsMutationEventJdbcRepository(jdbcConnector, fundsMutationSubjects);
        final PostponedCurrencyExchangeEventJdbcRepository postponedCurrencyExchangeEvents = new PostponedCurrencyExchangeEventJdbcRepository(jdbcConnector);
        final PostponedFundsMutationEventJdbcRepository postponedFundsMutationEvents = new PostponedFundsMutationEventJdbcRepository(jdbcConnector, fundsMutationEvents);
        final JdbcTreasury treasury = new JdbcTreasury(jdbcConnector);
        final SubjectPriceJdbcRepository subjectPrices = new SubjectPriceJdbcRepository(jdbcConnector, fundsMutationSubjects);

        accounter = new JdbcAccounter(this, jdbcConnector);

        order = ImmutableMap.<Repo, JdbcRepository>builder()
                .put(Repo.TREASURY, treasury)
                .put(Repo.FUNDS_MUTATION_SUBJECTS, fundsMutationSubjects)
                .put(Repo.FUNDS_MUTATION_AGENTS, fundsMutationAgents)
                .put(Repo.CURRENCY_RATES, currencyRates)
                .put(Repo.CURRENCY_EXCHANGE_EVENTS, currencyExchangeEvents)
                .put(Repo.FUNDS_MUTATION_EVENTS, fundsMutationEvents)
                .put(Repo.POSTPONED_CURRENCY_EXCHANGE_EVENTS, postponedCurrencyExchangeEvents)
                .put(Repo.POSTPONED_FUNDS_MUTATION_EVENTS, postponedFundsMutationEvents)
                .put(Repo.SUBJECT_PRICES, subjectPrices)
                .build();
    }

    private void executeMultipleSql(JdbcTemplate jdbcTemplate, String[] createTableSql, @Nullable Logger logger) {
        for (String sql : createTableSql) {
            if (sql != null) {
                if (logger != null) {
                    logger.info(sql);
                }
                if (jdbcConnector.transactionalSupport != null) {
                    jdbcConnector.transactionalSupport.runWithTransaction(() -> jdbcTemplate.execute(sql));
                    try {
                        Thread.sleep(RANDOM.nextInt(6) + 5); // Sadly, Sqlite breaks down without this in multiple concurrent connections case (random is not required);
                        // but no worries - this method is only for DDL
                    } catch (InterruptedException ignore) {
                        if (logger != null) {
                            logger.error("Sleep error", ignore);
                        }
                    }
                } else {
                    jdbcTemplate.execute(sql);
                }
            }
        }
    }

    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        for (final JdbcRepository repository : order.values()) {
            repository.setSqlDialect(sqlDialect);
        }
        accounter.setSqlDialect(sqlDialect);
    }

    public void setNewDataSource(DataSource dataSource) {
        jdbcConnector.setDataSource(dataSource, null);
    }

    public void setNewDataSource(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        jdbcConnector.setDataSource(dataSource, txSupport);
    }

    @Override
    @Nullable
    public JdbcTransactionalSupport getTransactionalSupport() {
        return jdbcConnector.transactionalSupport;
    }

    @Override
    public void setTransactionalSupport(@Nullable TransactionalSupport txSupport) {
        checkArgument(txSupport == null || txSupport instanceof JdbcTransactionalSupport,
                "This implementation will only work with JdbcTransactionalSupport instances");
        jdbcConnector.transactionalSupport = (JdbcTransactionalSupport) txSupport;
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
    public SubjectPriceRepository subjectPrices() {
        return (SubjectPriceRepository) order.get(Repo.SUBJECT_PRICES);
    }

    @Override
    public void clearSchema() {
        final JdbcTemplate jdbcTemplate = jdbcConnector.getJdbcTemplate();
        for (final JdbcRepository repo : order.values().asList().reverse()) {
            executeMultipleSql(jdbcTemplate, repo.getDropTableSql(), logger);
        }
        createSchema(jdbcTemplate);
    }

    @Override
    public void createSchemaIfNeeded() {
        final JdbcTemplate jdbcTemplate = jdbcConnector.getJdbcTemplate();
        if (jdbcTemplate.query(sqlDialect.tableExistsSql(FundsMutationAgentJdbcRepository.TABLE_NAME), Common.STRING_ROW_MAPPER).isEmpty()) {
            createSchema(jdbcTemplate);
        }
    }

    private void createSchema(JdbcTemplate jdbcTemplate) {
        for (final JdbcRepository repo : order.values()) {
            executeMultipleSql(jdbcTemplate, repo.getCreateTableSql(), logger);
        }
    }

    @Override
    public void clear(Repo repo) {
        clearRepo(jdbcConnector.getJdbcTemplate(), order.get(repo));
    }

    private void clearRepo(JdbcTemplate jdbcTemplate, JdbcRepository repository) {
        executeMultipleSql(jdbcTemplate, repository.getDropTableSql(), logger);
        executeMultipleSql(jdbcTemplate, repository.getCreateTableSql(), logger);
    }

}
