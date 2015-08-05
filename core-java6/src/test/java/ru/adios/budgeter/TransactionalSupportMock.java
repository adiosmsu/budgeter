package ru.adios.budgeter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 6/16/15
 * Time: 9:58 PM
 *
 * @author Mikhail Kulikov
 */
public class TransactionalSupportMock implements TransactionalSupport {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalSupportMock.class);

    @Override
    public void runWithTransaction(Runnable runnable) {
        logger.debug("TransactionalSupportMock run {}", runnable);
        runnable.run();
    }

}
