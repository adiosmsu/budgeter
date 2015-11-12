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

package ru.adios.budgeter.api;

import java.io.Serializable;

/**
 * Date: 10/24/15
 * Time: 7:51 PM
 *
 * @author Mikhail Kulikov
 */
public enum Order implements Serializable {

    ASC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return compareResult;
        }

        @Override
        public Order other() {
            return DESC;
        }
    }, DESC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return -compareResult;
        }

        @Override
        public Order other() {
            return ASC;
        }
    };

    public abstract int applyToCompareResult(int compareResult);

    public abstract Order other();

}
