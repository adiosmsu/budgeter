package ru.adios.budgeter.inmemrepo;

/**
 * Date: 6/15/15
 * Time: 10:10 AM
 *
 * @author Mikhail Kulikov
 */
class Stored<T> {

    public final int id;
    public final T obj;

    public Stored(int id, T obj) {
        this.id = id;
        this.obj = obj;
    }

}
