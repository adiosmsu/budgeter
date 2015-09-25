package ru.adios.budgeter.api;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 9/25/15
 * Time: 4:17 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class BudgeterApiException extends RuntimeException {

    BudgeterApiException(Throwable cause) {
        super(cause);
    }

    BudgeterApiException(String message) {
        super(message);
    }

    BudgeterApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
