package ru.adios.budgeter.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Date: 10/24/15
 * Time: 7:43 PM
 *
 * @author Mikhail Kulikov
 */
public final class RepoUtil {

    public static final class Pair<T extends OrderedField> {
        final Optional<OptLimit> limit;
        final List<OrderBy<T>> options;

        private Pair(@Nullable OptLimit limit, @Nonnull List<OrderBy<T>> options) {
            this.limit = Optional.ofNullable(limit);
            this.options = options;
        }
    }

    public static <T extends OrderedField> Pair<T> parseOptVarArg(RepoOption[] options, Class<T> clazz) {
        final ArrayList<OrderBy<T>> repoOptions = new ArrayList<>(options.length);
        OptLimit limit = null;

        for (final RepoOption repoOption : options) {
            if (repoOption instanceof OptLimit) {
                limit = (OptLimit) repoOption;
            } else if (repoOption instanceof OrderBy) {
                final OrderBy orderBy = (OrderBy) repoOption;
                if (clazz.isAssignableFrom(orderBy.field.getClass())) {
                    //noinspection unchecked
                    repoOptions.add((OrderBy<T>) repoOption);
                }
            }
        }

        return new Pair<>(limit, repoOptions);
    }

}
