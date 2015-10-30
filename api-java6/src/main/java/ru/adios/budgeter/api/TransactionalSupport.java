package ru.adios.budgeter.api;

/**
 * Date: 6/16/15
 * Time: 6:22 PM
 *
 * @author Mikhail Kulikov
 */
public interface TransactionalSupport {

    void runWithTransaction(Runnable runnable);

}
