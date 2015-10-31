package ru.adios.budgeter.jdbcrepo;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 11/1/15
 * Time: 12:06 AM
 *
 * @author Mikhail Kulikov
 */
public abstract class AgnosticPartialRowMapper<T> extends AgnosticRowMapper<T> {

    @Override
    public final T mapRow(ResultSet rs) throws SQLException {
        return mapRowStartingFrom(1, rs);
    }

    abstract T mapRowStartingFrom(int start, ResultSet rs) throws SQLException;


}
