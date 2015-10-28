package ru.adios.budgeter.jdbcrepo;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 10/28/15
 * Time: 7:18 AM
 *
 * @author Mikhail Kulikov
 */
public interface AgnosticPartialRowMapper<T> extends AgnosticRowMapper<T> {

    @Override
    default T mapRow(ResultSet rs) throws SQLException {
        return mapRowStartingFrom(1, rs);
    }

    T mapRowStartingFrom(int start, ResultSet rs) throws SQLException;

}
