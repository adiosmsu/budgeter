package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/26/15
 * Time: 7:35 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
final class SafeJdbcTemplateProvider {

    private final ThreadLocal<JdbcTemplate> templateThreadLocal = new ThreadLocal<>();

    private volatile DataSource dataSource;

    SafeJdbcTemplateProvider(DataSource dataSource) {
        setDataSource(dataSource);
    }

    void setDataSource(DataSource dataSource) {
        checkNotNull(dataSource, "dataSource is null");
        this.dataSource = dataSource;
        templateThreadLocal.set(new JdbcTemplate(dataSource));
    }

    JdbcTemplate get() {
        JdbcTemplate jdbcTemplate = templateThreadLocal.get();
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
            templateThreadLocal.set(jdbcTemplate);
        }
        return jdbcTemplate;
    }

}
