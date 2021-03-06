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

import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/26/15
 * Time: 7:35 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
final class SafeJdbcConnector {

    // typically you don't need more than one instance of this class so ThreadLocal being static here is ok
    private final ThreadLocal<JdbcTemplate> templateThreadLocal = new ThreadLocal<JdbcTemplate>();
    private volatile DataSource dataSource;

    volatile JdbcTransactionalSupport transactionalSupport;

    SafeJdbcConnector(DataSource dataSource) {
        setDataSource(dataSource, null);
    }

    SafeJdbcConnector(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        setDataSource(dataSource, txSupport);
    }

    void setDataSource(DataSource dataSource, @Nullable JdbcTransactionalSupport txSupport) {
        checkNotNull(dataSource, "dataSource is null");
        this.dataSource = dataSource;
        transactionalSupport = txSupport;
        templateThreadLocal.set(new JdbcTemplate(dataSource));
    }

    JdbcTemplate getJdbcTemplate() {
        JdbcTemplate jdbcTemplate = templateThreadLocal.get();
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.setExceptionTranslator(Common.EXCEPTION_TRANSLATOR);
            templateThreadLocal.set(jdbcTemplate);
        }
        return jdbcTemplate;
    }

    JdbcConnectionHolder getConnection() throws SQLException {
        final JdbcTransactionalSupport txSupport = transactionalSupport;
        final JdbcConnectionHolder ret;
        if (txSupport != null) {
            ret = txSupport.getConnection(dataSource);
        } else {
            ret = new JdbcConnectionHolder(dataSource.getConnection(), false);
        }
        return ret;
    }

    void releaseConnection(JdbcConnectionHolder con) throws SQLException {
        final JdbcTransactionalSupport txSupport = transactionalSupport;
        if (txSupport != null) {
            txSupport.releaseConnection(con, dataSource);
        } else {
            if (con.isTransactional) {
                JdbcTransactionalSupport.Static.defaultRelease(con.connection, dataSource);
            } else {
                con.connection.close();
            }
        }
    }

}
