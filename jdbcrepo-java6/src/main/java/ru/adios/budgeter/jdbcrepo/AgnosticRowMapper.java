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
