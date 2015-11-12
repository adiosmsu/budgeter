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

import org.springframework.jdbc.datasource.DataSourceUtils;
import ru.adios.budgeter.api.TransactionalSupport;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Date: 10/29/15
 * Time: 6:16 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public interface JdbcTransactionalSupport extends TransactionalSupport {

    default JdbcConnectionHolder getConnection(DataSource dataSource) {
        return new JdbcConnectionHolder(DataSourceUtils.getConnection(dataSource), true);
    }

    default void releaseConnection(JdbcConnectionHolder con, DataSource dataSource) {
        if (con.isTransactional) {
            defaultRelease(con.connection, dataSource);
        } else {
            try {
                con.connection.close();
            } catch (SQLException ex) {
                throw Common.EXCEPTION_TRANSLATOR.translate("JdbcTransactionalSupport.releaseConnection", null, ex);
            }
        }
    }

    static void defaultRelease(Connection con, DataSource dataSource) {
        DataSourceUtils.releaseConnection(con, dataSource);
    }

}
