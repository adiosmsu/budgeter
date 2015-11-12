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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Date: 10/24/15
 * Time: 7:31 PM
 *
 * @author Mikhail Kulikov
 */
public final class OptLimit implements RepoOption {

    public static OptLimit create(int limit, int offset) {
        return new OptLimit(limit, offset);
    }

    public static OptLimit createLimit(int limit) {
        return new OptLimit(limit, Type.LIMIT);
    }

    public static OptLimit createOffset(int offset) {
        return new OptLimit(offset, Type.OFFSET);
    }

    public final int limit;
    public final int offset;

    private OptLimit(int limit, int offset) {
        this.limit = limit;
        checkLimit();
        this.offset = offset;
        checkOffset();
    }

    private OptLimit(int value, Type type) {
        switch (type) {
            case LIMIT:
                this.limit = value;
                this.offset = -1;
                checkLimit();
                return;
            case OFFSET:
                this.offset = value;
                this.limit = -1;
                checkOffset();
                return;
            default:
                throw new IllegalStateException("Unreachable");
        }
    }

    private void checkLimit() {
        checkArgument(limit >= 0, "Limit must be positive");
    }

    private void checkOffset() {
        checkArgument(offset >= 0, "Offset must be positive");
    }

    private enum Type {
        LIMIT, OFFSET
    }

}
