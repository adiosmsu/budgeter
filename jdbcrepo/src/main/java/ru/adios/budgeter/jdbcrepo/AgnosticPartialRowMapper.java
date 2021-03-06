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
