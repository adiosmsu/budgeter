package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 10/27/15
 * Time: 5:06 PM
 *
 * @author Mikhail Kulikov
 */
public interface AgnosticRowMapper<T> extends RowMapper<T> {

    @Override
    default T mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs);
    }

    T mapRow(ResultSet rs) throws SQLException;

}
