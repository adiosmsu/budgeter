package ru.adios.budgeter.inmemrepo;

import java8.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

/**
 * Date: 6/15/15
 * Time: 1:21 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class AbstractPseudoTable<T extends Stored<U>, U> implements PseudoTable<T> {

    @Nonnull
    abstract ConcurrentHashMap<Integer, T> innerTable();

    @Override
    public final T get(int id) {
        return innerTable().get(id);
    }

    @Override
    public void clear() {
        innerTable().clear();
    }

}
