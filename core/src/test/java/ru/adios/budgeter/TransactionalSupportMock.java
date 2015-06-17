package ru.adios.budgeter;

/**
 * Date: 6/16/15
 * Time: 9:58 PM
 *
 * @author Mikhail Kulikov
 */
public class TransactionalSupportMock implements TransactionalSupport {

    @Override
    public void runWithTransaction(Runnable runnable) {
        runnable.run();
    }

}
