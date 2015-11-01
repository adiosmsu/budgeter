package ru.adios.budgeter.jdbcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Date: 10/31/15
 * Time: 10:32 PM
 *
 * @author Mikhail Kulikov
 */
interface Wrappable extends AutoCloseable {

    Logger logger = LoggerFactory.getLogger(Wrappable.class);

    static void closeSilently(AutoCloseable delegate) {
        try {
            delegate.close();
        } catch (Exception ignore) {
            logger.debug("Delegate stream close exception", ignore);
        }
    }

    default <ReturnType, Param> ReturnType wrap(Function<Param, ReturnType> f, Param p) {
        try {
            return f.apply(p);
        } finally {
            closeSilently(this);
        }
    }

    default <Param> void wrap(Param p, Consumer<Param> c) {
        try {
            c.accept(p);
        } finally {
            closeSilently(this);
        }
    }

    default <ReturnType> ReturnType wrap(Supplier<ReturnType> f) {
        try {
            return f.get();
        } finally {
            closeSilently(this);
        }
    }

}
