package ru.adios.budgeter;

import org.slf4j.Logger;
import ru.adios.budgeter.api.TransactionalSupport;

import java.util.function.Supplier;

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
