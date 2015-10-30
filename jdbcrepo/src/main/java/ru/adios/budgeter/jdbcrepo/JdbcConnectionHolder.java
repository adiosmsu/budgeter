package ru.adios.budgeter.jdbcrepo;

import javax.annotation.concurrent.Immutable;
import java.sql.Connection;

/**
 * Date: 10/30/15
 * Time: 1:44 PM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class JdbcConnectionHolder {

    final Connection connection;
    final boolean isTransactional;

    public JdbcConnectionHolder(Connection connection, boolean isTransactional) {
        this.connection = connection;
        this.isTransactional = isTransactional;
    }

}
