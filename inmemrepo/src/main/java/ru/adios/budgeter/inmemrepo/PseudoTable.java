package ru.adios.budgeter.inmemrepo;

/**
 * Date: 6/15/15
 * Time: 1:33 PM
 *
 * @author Mikhail Kulikov
 */
public interface PseudoTable<T> {

    T get(int id);

    void clear();

}
