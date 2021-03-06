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

import com.google.common.base.Throwables;

/**
 * Date: 10/29/15
 * Time: 4:39 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class TestCheckedRunnable implements Runnable {

    @Override
    public void run() {
        try {
            runChecked();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public abstract void runChecked() throws Exception;

}
