package ru.adios.budgeter.jdbcrepo;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Date: 10/29/15
 * Time: 3:21 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
class ResultSetSupplier implements Supplier<ResultSet>, AutoCloseable {

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
            //noinspection SqlDialectInspection
            throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.get()", sql, e);
        }
    }

    protected void enrichStatement(PreparedStatement statement) throws SQLException {
    }

    @Override
    public void close() {
        try {
            try {
                statement.close();
            } catch (SQLException e) {
                try {
                    //noinspection SqlDialectInspection
                    throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.close()", sql, e);
                } finally {
                    try {
                        jdbcConnector.releaseConnection(connectionHolder);
                    } catch (Exception ignore) {}
                }
            }
            jdbcConnector.releaseConnection(connectionHolder);
        } catch (SQLException e) {
            //noinspection SqlDialectInspection
            throw Common.EXCEPTION_TRANSLATOR.translate(op != null ? op : "ResultSetSupplier.close()", sql, e);
        }
    }

}
