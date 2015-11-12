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

package ru.adios.budgeter;

import java8.util.function.Supplier;
import org.slf4j.Logger;
import ru.adios.budgeter.api.TransactionalSupport;

/**
 * Date: 11/3/15
 * Time: 10:49 PM
 *
 * @author Mikhail Kulikov
 */
public final class SubmitHelper<T> {

    private final Logger logger;
    private final String errorMessage;

    public SubmitHelper(Logger logger, String errorMessage) {
        this.logger = logger;
        this.errorMessage = errorMessage;
    }

    private TransactionalSupport transactionalSupport;

    public TransactionalSupport getTransactionalSupport() {
        return transactionalSupport;
    }

    public void setTransactionalSupport(TransactionalSupport transactionalSupport) {
        this.transactionalSupport = transactionalSupport;
    }

    public Submitter.Result<T> doSubmit(Supplier<Submitter.Result<T>> supplier, Submitter.ResultBuilder<T> resultBuilder) {
        try {
            if (transactionalSupport != null) {
                return transactionalSupport.getWithTransaction(supplier);
            } else {
                return supplier.get();
            }
        } catch (RuntimeException ex) {
            logger.error(errorMessage, ex);
            return resultBuilder
                    .setGeneralError(errorMessage + ": " + ex.getMessage())
                    .build();
        }
    }

}
