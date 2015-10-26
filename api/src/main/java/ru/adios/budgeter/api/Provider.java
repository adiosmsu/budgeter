package ru.adios.budgeter.api;

import java.util.Optional;

/**
 * Date: 10/26/15
 * Time: 3:03 PM
 *
 * @author Mikhail Kulikov
 */
public interface Provider<T, IdType> {

    Optional<T> getById(IdType id);

    IdType currentSeqValue();

}
