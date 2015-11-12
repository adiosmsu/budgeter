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

import java8.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 10/29/15
 * Time: 3:21 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
class ResultSetSupplier implements Supplier<ResultSet>, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ResultSetSupplier.class);

    private final SafeJdbcConnector jdbcConnector;
    private final String sql;
    @Nullable
    private final String op;

    private PreparedStatement statement;
    private JdbcConnectionHolder connectionHolder;

    ResultSetSupplier(SafeJdbcConnector jdbcConnector, String sql) {
        this(jdbcConnector, sql, null);
    }

    ResultSetSupplier(SafeJdbcConnector jdbcConnector, String sql, @Nullable String op) {
        this.jdbcConnector = jdbcConnector;
        this.sql = sql;
        this.op = op;
    }

    @Override
    public ResultSet get() {
        try {
            connectionHolder = jdbcConnector.getConnection();
            statement = connectionHolder.connection.prepareStatement(sql);
            enrichStatement(statement);
            return statement.executeQuery();
        } catch (SQLException e) {
            closeInner(true);
            //noinspection SqlDialectInspection
            throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.get()", sql, e);
        }
    }

    protected void enrichStatement(PreparedStatement statement) throws SQLException {
    }

    @Override
    public void close() {
        closeInner(false);
    }

    private void closeInner(boolean eatException) {
        try {
            try {
                statement.close();
            } catch (SQLException e) {
                try {
                    if (eatException) {
                        logger.debug("Statement close threw exception", e);
                    } else {
                        //noinspection SqlDialectInspection
                        throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.close()", sql, e);
                    }
                } finally {
                    try {
                        jdbcConnector.releaseConnection(connectionHolder);
                    } catch (Exception ignore) {
                        logger.debug("Release connection after statement close failure also threw exception", ignore);
                    }
                }
            }

            jdbcConnector.releaseConnection(connectionHolder);
        } catch (SQLException e) {
            if (eatException) {
                logger.debug("Release connection threw exception", e);
            } else {
                //noinspection SqlDialectInspection
                throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.close()", sql, e);
            }
        }
    }

}
