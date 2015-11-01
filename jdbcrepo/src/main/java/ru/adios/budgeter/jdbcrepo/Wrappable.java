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

    Logger logger = LoggerFactory.getLogger(ClosingOnTerminalOpsStream.class);

    static <T> void closeSilently(AutoCloseable delegate) {
        try {
            delegate.close();
        } catch (Exception ignore) {
            logger.debug("Delegate stream close exception", ignore);
        }
    }

    static <ReturnType, Param> ReturnType wrapWithClose(AutoCloseable s, Function<Param, ReturnType> f, Param p) {
        try {
            return f.apply(p);
        } finally {
            closeSilently(s);
        }
    }

    static <Param> void wrapWithClose(AutoCloseable s, Consumer<Param> c, Param p) {
        try {
            c.accept(p);
        } finally {
            closeSilently(s);
        }
    }

    static <ReturnType> ReturnType wrapWithClose(AutoCloseable s, Supplier<ReturnType> f) {
        try {
            return f.get();
        } finally {
            closeSilently(s);
        }
    }

    default <ReturnType, Param> ReturnType wrap(Function<Param, ReturnType> f, Param p) {
        return wrapWithClose(this, f, p);
    }

    default <Param> void wrap(Param p, Consumer<Param> c) {
        wrapWithClose(this, c, p);
    }

    default <ReturnType> ReturnType wrap(Supplier<ReturnType> f) {
        return wrapWithClose(this, f);
    }

}
