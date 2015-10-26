package ru.adios.budgeter.api;

/**
 * Date: 10/26/15
 * Time: 6:47 PM
 *
 * @author Mikhail Kulikov
 */
public interface Repository<IdType> {

    void setSequenceValue(IdType value);

}
