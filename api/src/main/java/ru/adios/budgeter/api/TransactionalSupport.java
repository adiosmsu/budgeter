package ru.adios.budgeter.api;

import java.util.function.Supplier;

/**
 * Date: 6/16/15
 * Time: 6:22 PM
 *
 * @author Mikhail Kulikov
 */
public interface TransactionalSupport {

    void runWithTransaction(Runnable runnable);

    <T> T getWithTransaction(Supplier<T> supplier);

}
