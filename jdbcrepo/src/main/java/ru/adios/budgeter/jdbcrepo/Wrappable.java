package ru.adios.budgeter.jdbcrepo;

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

    static <ReturnType, Param> ReturnType wrapWithClose(AutoCloseable s, Function<Param, ReturnType> f, Param p) {
        try {
            return f.apply(p);
        } finally {
            ClosingOnTerminalOpsStream.closeSilently(s);
        }
    }

    static <Param> void wrapWithClose(AutoCloseable s, Consumer<Param> c, Param p) {
        try {
            c.accept(p);
        } finally {
            ClosingOnTerminalOpsStream.closeSilently(s);
        }
    }

    static <ReturnType> ReturnType wrapWithClose(AutoCloseable s, Supplier<ReturnType> f) {
        try {
            return f.get();
        } finally {
            ClosingOnTerminalOpsStream.closeSilently(s);
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
