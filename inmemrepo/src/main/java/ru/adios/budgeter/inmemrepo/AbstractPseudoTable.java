package ru.adios.budgeter.inmemrepo;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 6/15/15
 * Time: 1:21 PM
 *
 * @author Mikhail Kulikov
 */
public abstract class AbstractPseudoTable<T extends Stored<U>, U> implements PseudoTable<U> {

    @Nonnull
    abstract ConcurrentHashMap<Integer, T> innerTable();

    @Override
    public final U get(int id) {
        return innerTable().get(id).obj;
    }

    @Override
    public void clear() {
        innerTable().clear();
    }

}
