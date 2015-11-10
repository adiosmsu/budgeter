package ru.adios.budgeter.jdbcrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import org.intellij.lang.annotations.Language;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.*;

import javax.annotation.concurrent.ThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 10/29/15
 * Time: 5:29 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class JdbcAccounter implements Accounter {

    @SuppressWarnings("SqlDialectInspection")
    @Language("SQL")
    private static final String COMPAT_SQL =
            "SELECT DISTINCT e.day, e.unit AS currency_unit" +
                    " FROM postponed_funds_mutation_event e" +
                    " UNION ALL" +
                    " SELECT DISTINCT pc.day, s.currency_unit" +
                    " FROM postponed_currency_exchange_event pc" +
                    " INNER JOIN balance_account s ON pc.sell_account_id = s.id" +
                    " UNION ALL" +
                    " SELECT DISTINCT pc.day, b.currency_unit" +
                    " FROM postponed_currency_exchange_event pc" +
                    " INNER JOIN balance_account b ON pc.to_buy_account_id = b.id" +
                    " UNION ALL" +
                    " SELECT DISTINCT e.day, e.conversion_unit AS currency_unit" +
                    " FROM postponed_funds_mutation_event e " +
                    "UNION ALL " +
                    "SELECT " + Long.MAX_VALUE + " AS day, " + CurrencyUnit.USD.getNumericCode() + " AS currency_unit " +
                    "ORDER BY day, currency_unit";

    @SuppressWarnings("SqlDialectInspection")
    @Language("SQL")
    private static final String SQL =
            "WITH large AS (" +
                    "SELECT DISTINCT e.day, e.unit AS currency_unit" +
                    " FROM postponed_funds_mutation_event e" +
                    " UNION ALL" +
                    " SELECT DISTINCT pc.day, s.currency_unit" +
                    " FROM postponed_currency_exchange_event pc" +
                    " INNER JOIN balance_account s ON pc.sell_account_id = s.id" +
                    " UNION ALL" +
                    " SELECT DISTINCT pc.day, b.currency_unit" +
                    " FROM postponed_currency_exchange_event pc" +
                    " INNER JOIN balance_account b ON pc.to_buy_account_id = b.id" +
                    " UNION ALL" +
                    " SELECT DISTINCT e.day, e.conversion_unit AS currency_unit" +
                    " FROM postponed_funds_mutation_event e) " +
                    "SELECT DISTINCT l.day, l.currency_unit FROM large l " +
                    "UNION ALL " +
                    "SELECT " + Long.MAX_VALUE + " AS day, " + CurrencyUnit.USD.getNumericCode() + " AS currency_unit " +
                    "ORDER BY day, currency_unit"; // we will rely on data to be ordered so we can imitate partial reduce with Stream API using filter()


    private final SourcingBundle bundle;
    private final SafeJdbcConnector jdbcConnector;
    private SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    JdbcAccounter(SourcingBundle bundle, SafeJdbcConnector jdbcConnector) {
        this.bundle = bundle;
        this.jdbcConnector = jdbcConnector;
    }


    void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEventRepository() {
        return bundle.currencyExchangeEvents();
    }

    @Override
    public FundsMutationEventRepository fundsMutationEventRepository() {
        return bundle.fundsMutationEvents();
    }

    @Override
    public SubjectPriceRepository subjectPriceRepository() {
        return bundle.subjectPrices();
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEventRepository() {
        return bundle.postponedFundsMutationEvents();
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository() {
        return bundle.postponedCurrencyExchangeEvents();
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return bundle.fundsMutationSubjects();
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgentRepo() {
        return bundle.fundsMutationAgents();
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons(boolean compatMode) {
        if (compatMode) {
            return getPostponingReasonsStream(COMPAT_SQL, true);
        } else {
            return streamAllPostponingReasons();
        }
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        return getPostponingReasonsStream(SQL, false);
    }

    private Stream<PostponingReasons> getPostponingReasonsStream(String sql, boolean doDistinctAfterFirstStream) {
        final AccumulationContext context = new AccumulationContext();
        final LazyResultSetIterator<Pair> iterator = LazyResultSetIterator.of(
                Common.getRsSupplier(jdbcConnector, sql, "streamAllPostponingReasons"),
                Common.getMappingSqlFunction(
                        new AgnosticRowMapper<Pair>() {
                            @Override
                            Pair mapRow(ResultSet rs) throws SQLException {
                                return new Pair(sqlDialect.translateFromDb(rs.getObject(1), UtcDay.class), CurrencyUnit.ofNumericCode(rs.getInt(2)));
                            }
                        },
                        sql, "streamAllPostponingReasons"
                ),
                sql
        );
        Stream<Pair> stream = iterator.stream();
        if (doDistinctAfterFirstStream) {
            stream = stream.distinct();
        }
        return stream
                .filter(new Predicate<Pair>() {
                    @Override
                    public boolean test(Pair pair) {
                        if (context.builder == null) {
                            context.builder = ImmutableSet.builder();
                            context.currentDay = pair.day;
                            context.builder.add(pair.unit);
                            return false;
                        }

                        if (!pair.day.equals(context.currentDay)) {
                            return true;
                        } else {
                            context.builder.add(pair.unit);
                            return false;
                        }
                    }
                })
                .map(new Function<Pair, PostponingReasons>() {
                    @Override
                    public PostponingReasons apply(Pair pair) {
                        final PostponingReasons postponingReasons = new PostponingReasons(context.currentDay, context.builder.build());
                        if (iterator.hasNext()) {
                            context.builder = ImmutableSet.builder();
                            context.currentDay = pair.day;
                            context.builder.add(pair.unit);
                        }
                        return postponingReasons;
                    }
                });
    }

    private static final class AccumulationContext {

        private ImmutableSet.Builder<CurrencyUnit> builder;
        private UtcDay currentDay;

    }

    private static final class Pair {

        private final UtcDay day;
        private final CurrencyUnit unit;

        private Pair(UtcDay day, CurrencyUnit unit) {
            this.day = day;
            this.unit = unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            return day.equals(pair.day)
                    && unit.equals(pair.unit);
        }

        @Override
        public int hashCode() {
            int result = day.hashCode();
            result = 31 * result + unit.hashCode();
            return result;
        }

    }

}
