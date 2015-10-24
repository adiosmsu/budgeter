package ru.adios.budgeter.api;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/24/15
 * Time: 8:10 PM
 *
 * @author Mikhail Kulikov
 */
public final class OrderBy<T extends OrderedField> implements RepoOption {

    public final T field;
    public final Order order;

    public OrderBy(T field, Order order) {
        checkNotNull(order, "order");
        checkNotNull(field, "field");
        this.field = field;
        this.order = order;
    }

}
