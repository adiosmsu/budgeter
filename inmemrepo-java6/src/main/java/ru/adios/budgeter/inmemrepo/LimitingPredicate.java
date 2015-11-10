package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.function.Predicate;
import ru.adios.budgeter.api.OptLimit;

/**
 * Date: 11/9/15
 * Time: 12:17 PM
 *
 * @author Mikhail Kulikov
 */
public class LimitingPredicate<T> implements Predicate<T> {

    private final Optional<OptLimit> limitRef;

    private int offsetCounter;
    private int limitCounter;

    public LimitingPredicate(Optional<OptLimit> limitRef) {
        this.limitRef = limitRef;
    }

    @Override
    public boolean test(T t) {
        if (!limitRef.isPresent()) {
            return true;
        }
        final OptLimit limit = limitRef.get();
        return !(limit.offset > 0 && limit.offset > offsetCounter++)
                && !(limit.limit > 0 && limit.limit < ++limitCounter);
    }

}
