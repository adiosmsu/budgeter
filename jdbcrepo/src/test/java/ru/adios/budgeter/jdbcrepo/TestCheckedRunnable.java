package ru.adios.budgeter.jdbcrepo;

import com.google.common.base.Throwables;

/**
 * Date: 10/29/15
 * Time: 4:39 PM
 *
 * @author Mikhail Kulikov
 */
public interface TestCheckedRunnable extends Runnable {

    @Override
    default void run() {
        try {
            runChecked();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    void runChecked() throws Exception;

}
