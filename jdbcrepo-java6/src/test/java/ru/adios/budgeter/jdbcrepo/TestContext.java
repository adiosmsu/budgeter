/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.jdbcrepo;

import com.jolbox.bonecp.BoneCPDataSource;
import java8.util.function.Supplier;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Date: 10/29/15
 * Time: 11:26 AM
 *
 * @author Mikhail Kulikov
 */
public final class TestContext {

    public static final SourcingBundle BUNDLE;
    public static final JdbcTransactionalSupport TRANSACTIONAL_SUPPORT;

    static {
        final BoneCPDataSource dataSource = getBoneCPDataSource();

        final TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        TRANSACTIONAL_SUPPORT = new JdbcTransactionalSupport() {
            @Override
            public JdbcConnectionHolder getConnection(DataSource dataSource) {
                return JdbcTransactionalSupport.Static.getConnection(dataSource);
            }

            @Override
            public void releaseConnection(JdbcConnectionHolder con, DataSource dataSource) {
                JdbcTransactionalSupport.Static.releaseConnection(con, dataSource);
            }

            @Override
            public void runWithTransaction(final Runnable runnable) {
                txTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        runnable.run();
                    }
                });
            }

            @Override
            public <T> T getWithTransaction(final Supplier<T> supplier) {
                return txTemplate.execute(new TransactionCallback<T>() {
                    @Override
                    public T doInTransaction(TransactionStatus status) {
                        return supplier.get();
                    }
                });
            }
        };
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
