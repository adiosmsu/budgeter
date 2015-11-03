package ru.adios.budgeter.jdbcrepo;

import com.jolbox.bonecp.BoneCPDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Date: 10/29/15
 * Time: 11:26 AM
 *
 * @author Mikhail Kulikov
 */
public final class TestContext {

    public static void ex(TestCheckedRunnable r) {
        BUNDLE.tryExecuteInTransaction(r);
    }

    public static final SourcingBundle BUNDLE;
    public static final JdbcTransactionalSupport TRANSACTIONAL_SUPPORT;

    static {
        final BoneCPDataSource dataSource = getBoneCPDataSource();

        final TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        TRANSACTIONAL_SUPPORT = runnable -> txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                runnable.run();
            }
        });
        BUNDLE = new SourcingBundle(dataSource, TRANSACTIONAL_SUPPORT);
        BUNDLE.clearSchema();
    }

    @Nonnull
    static BoneCPDataSource getBoneCPDataSource() {
        final BoneCPDataSource dataSource = new BoneCPDataSource();
        //dataSource.setJdbcUrl("jdbc:sqlite:tests.db");
        dataSource.setJdbcUrl("jdbc:sqlite::memory:");
        dataSource.setDriverClass("org.sqlite.JDBC");
        dataSource.setDefaultAutoCommit(false);
        dataSource.setPartitionCount(1);
        dataSource.setMinConnectionsPerPartition(1);
        dataSource.setMaxConnectionsPerPartition(1);
        dataSource.setAcquireIncrement(1);
        //dataSource.setStatementsCacheSize(100);
        dataSource.setDisableConnectionTracking(true);
        dataSource.setConnectionTimeout(30, TimeUnit.SECONDS);
        dataSource.setInitSQL("select 1");
        return dataSource;
    }

}
