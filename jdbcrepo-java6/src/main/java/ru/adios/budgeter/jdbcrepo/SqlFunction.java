/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.jdbcrepo;

import java8.util.function.Function;

import javax.annotation.Nullable;
import java.sql.SQLException;

/**
 * Date: 10/27/15
 * Time: 4:31 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class SqlFunction<T, R> implements Function<T, R> {

    public static <T, R> SqlFunction<T, R> getVerboseFunction(final @Nullable String sql, final @Nullable String opName, final SqlFunction<T, R> function) {
        return new SqlFunction<T, R>() {
            @Override
            public R applySql(T o) throws SQLException {
                return function.applySql(o);
            }

            @Override
            public String opName() {
                return opName != null ? opName : super.opName();
            }

            @Nullable
            @Override
            public String sql() {
                return sql;
            }
        };
    }

    @Override
    public R apply(T t) {
        try {
            return applySql(t);
        } catch (SQLException e) {
            throw Common.EXCEPTION_TRANSLATOR.translate(opName(), sql(), e);
        }
    }

    public abstract R applySql(T t) throws SQLException;

    public String opName() {
        return "SqlFunction.apply(T)";
    }

    @Nullable
    public String sql() {
        return null;
    }

}
