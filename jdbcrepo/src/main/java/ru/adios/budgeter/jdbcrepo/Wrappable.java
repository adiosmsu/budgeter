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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Date: 10/31/15
 * Time: 10:32 PM
 *
 * @author Mikhail Kulikov
 */
interface Wrappable extends AutoCloseable {

    Logger logger = LoggerFactory.getLogger(Wrappable.class);

    static void closeSilently(AutoCloseable delegate) {
        try {
            delegate.close();
        } catch (Exception ignore) {
            logger.debug("Delegate stream close exception", ignore);
        }
    }

    default <ReturnType, Param> ReturnType wrap(Function<Param, ReturnType> f, Param p) {
        try {
            return f.apply(p);
        } finally {
            closeSilently(this);
        }
    }

    default <Param> void wrap(Param p, Consumer<Param> c) {
        try {
            c.accept(p);
        } finally {
            closeSilently(this);
        }
    }

    default <ReturnType> ReturnType wrap(Supplier<ReturnType> f) {
        try {
            return f.get();
        } finally {
            closeSilently(this);
        }
    }

}
