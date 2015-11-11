package ru.adios.budgeter.api;

import java.io.Serializable;

/**
 * Date: 10/24/15
 * Time: 8:11 PM
 *
 * @author Mikhail Kulikov
 */
public interface OrderedField extends RepoOption, Serializable {

    String name();

}
