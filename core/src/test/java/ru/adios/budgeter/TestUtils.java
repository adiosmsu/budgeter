package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.jdbcrepo.JdbcConnectionHolder;
import ru.adios.budgeter.jdbcrepo.JdbcTransactionalSupport;
import ru.adios.budgeter.jdbcrepo.SourcingBundle;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Date: 03.07.15
 * Time: 22:46
 *
 * @author Mikhail Kulikov
 */
public final class TestUtils {

    public static final SourcingBundle JDBC_BUNDLE;

    static {
        final SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        //final SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite:tests.db", true);
        final TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        dataSource.setAutoCommit(false);

        JDBC_BUNDLE = new SourcingBundle(dataSource, new JdbcTransactionalSupport() {
            @Override
            public synchronized void runWithTransaction(Runnable runnable) {
                txTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        runnable.run();
                    }
                });
            }

            @Override
            public <T> T getWithTransaction(Supplier<T> supplier) {
                return txTemplate.execute(status -> supplier.get());
            }

            @Override
            public synchronized JdbcConnectionHolder getConnection(DataSource dataSource) {
                return JdbcTransactionalSupport.super.getConnection(dataSource);
            }

            @Override
            public synchronized void releaseConnection(JdbcConnectionHolder con, DataSource dataSource) {
                JdbcTransactionalSupport.super.releaseConnection(con, dataSource);
            }
        });
    }

    public static final String CASE_JDBC = "Jdbc sqlite case";
    public static final String CASE_INNER = "In-memory case";

    public static final UtcDay TODAY = new UtcDay();
    public static final UtcDay YESTERDAY = new UtcDay(TODAY.inner.minus(1, ChronoUnit.DAYS));
    public static final UtcDay DAY_BF_YESTER = new UtcDay(YESTERDAY.inner.minus(1, ChronoUnit.DAYS));

    public static final UtcDay JULY_3RD_2015 = new UtcDay(OffsetDateTime.of(2015, 7, 3, 0, 0, 0, 0, ZoneOffset.UTC));

    static BalanceAccount prepareBalance(Bundle bundle, CurrencyUnit unit) {
        final BalanceAccount account = new BalanceAccount("account" + unit.getCode(), unit, null);
        return bundle.treasury().registerBalanceAccount(account);
    }

    private TestUtils() {}

}
