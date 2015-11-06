package ru.adios.budgeter.api;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/24/15
 * Time: 8:10 PM
 *
 * @author Mikhail Kulikov
 */
public final class OrderBy<T extends OrderedField> implements RepoOption {

    public static OrderBy<OrderedFieldDefault> getDefault(String fieldName, Order order) {
        return new OrderBy<OrderedFieldDefault>(new OrderedFieldDefault(fieldName), order);
    }

    public final T field;
    public final Order order;

    public OrderBy(T field, Order order) {
        checkNotNull(order, "order");
        checkNotNull(field, "field");
        this.field = field;
        this.order = order;
    }

    public OrderBy<T> flipOrder() {
        return new OrderBy<T>(field, order.other());
    }

    public static final class OrderedFieldDefault implements OrderedField {

        private final String n;

        public OrderedFieldDefault(String n) {
            this.n = n;
        }

        @Override
        public String name() {
            return n;
        }

    }

}
