package ru.adios.budgeter;

import com.google.common.base.Throwables;

/**
 * Date: 10/29/15
 * Time: 4:39 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class TestCheckedRunnable implements Runnable {

    @Override
    public void run() {
        try {
            runChecked();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public abstract void runChecked() throws Exception;

}
