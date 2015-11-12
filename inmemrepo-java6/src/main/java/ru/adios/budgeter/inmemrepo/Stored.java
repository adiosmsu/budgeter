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

package ru.adios.budgeter.inmemrepo;

/**
 * Date: 6/15/15
 * Time: 10:10 AM
 *
 * @author Mikhail Kulikov
 */
class Stored<T> {

    public final int id;
    public final T obj;

    public Stored(int id, T obj) {
        this.id = id;
        this.obj = obj;
    }

    @Override
    public String toString() {
        return "Stored{" +
                "id=" + id +
                ", obj=" + obj +
                '}';
    }

}
