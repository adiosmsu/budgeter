package ru.adios.budgeter.jdbcrepo;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Date: 10/27/15
 * Time: 4:31 PM
 *
 * @author Mikhail Kulikov
 */
public interface SqlFunction<T, R> extends Function<T, R> {

    static <T, R> SqlFunction<T, R> getVerboseFunction(@Nullable String sql, @Nullable String opName, SqlFunction<T, R> function) {
        return new SqlFunction<T, R>() {
            @Override
            public R applySql(T o) throws SQLException {
                return function.applySql(o);
            }

            @Override
            public String opName() {
                return opName != null ? opName : SqlFunction.super.opName();
            }

            @Nullable
            @Override
            public String sql() {
                return sql;
            }
        };
    }

    default R apply(T t) {
        try {
            return applySql(t);
        } catch (SQLException e) {
            throw Common.EXCEPTION_TRANSLATOR.translate(opName(), sql(), e);
        }
    }

    R applySql(T t) throws SQLException;

    default String opName() {
        return "SqlFunction.apply(T)";
    }

    @Nullable
    default String sql() {
        return null;
    }

}
