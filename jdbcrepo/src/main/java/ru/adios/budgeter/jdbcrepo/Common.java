package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import java.util.List;
import java.util.Optional;

/**
 * Date: 10/26/15
 * Time: 9:52 PM
 *
 * @author Mikhail Kulikov
 */
class Common {

    static final SingleColumnRowMapper<Long> LONG_ROW_MAPPER = new SingleColumnRowMapper<>(Long.class);
    static final SingleColumnRowMapper<String> STRING_ROW_MAPPER = new SingleColumnRowMapper<>(String.class);

    static final SQLStateSQLExceptionTranslator EXCEPTION_TRANSLATOR = new SQLStateSQLExceptionTranslator();

    static <T> Optional<T> getSingleOptional(List<T> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    static void executeMultipleSql(JdbcTemplate jdbcTemplate, String[] createTableSql) {
        for (String sql : createTableSql) {
            jdbcTemplate.execute(sql);
        }
    }

}
