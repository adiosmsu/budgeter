package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.OptLimit;

import java.util.Optional;
import java.util.function.Predicate;

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
