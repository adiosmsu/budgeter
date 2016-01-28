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

import javax.annotation.Nullable;
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
        return prepareBalance(bundle, unit, null);
    }

    static BalanceAccount prepareBalance(Bundle bundle, CurrencyUnit unit, @Nullable  String suffix) {
        final BalanceAccount account = new BalanceAccount("account" + (suffix == null ? unit.getCode() : unit.getCode() + '_' + suffix), unit, null);
        return bundle.treasury().registerBalanceAccount(account);
    }

    private TestUtils() {}

}
