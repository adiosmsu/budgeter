package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.datasource.DataSourceUtils;
import ru.adios.budgeter.api.TransactionalSupport;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Date: 10/29/15
 * Time: 6:16 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public interface JdbcTransactionalSupport extends TransactionalSupport {

    final class Static {

        static void defaultRelease(Connection con, DataSource dataSource) {
            DataSourceUtils.releaseConnection(con, dataSource);
        }

        static public JdbcConnectionHolder getConnection(DataSource dataSource) {
            return new JdbcConnectionHolder(DataSourceUtils.getConnection(dataSource), true);
        }

        static public void releaseConnection(JdbcConnectionHolder con, DataSource dataSource) {
            if (con.isTransactional) {
                Static.defaultRelease(con.connection, dataSource);
            }
        }

    }

    JdbcConnectionHolder getConnection(DataSource dataSource);

    void releaseConnection(JdbcConnectionHolder con, DataSource dataSource);

}
