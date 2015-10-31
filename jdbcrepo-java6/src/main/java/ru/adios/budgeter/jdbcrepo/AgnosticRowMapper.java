package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 11/1/15
 * Time: 12:05 AM
 *
 * @author Mikhail Kulikov
 */
public abstract class AgnosticRowMapper<T> implements RowMapper<T> {

    @Override
    public final T mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs);
    }

    abstract T mapRow(ResultSet rs) throws SQLException;

}
