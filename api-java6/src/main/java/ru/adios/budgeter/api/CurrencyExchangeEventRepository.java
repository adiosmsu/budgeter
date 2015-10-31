package ru.adios.budgeter.api;

import java8.util.stream.Stream;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Date: 6/15/15
 * Time: 9:52 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyExchangeEventRepository extends Provider<CurrencyExchangeEvent, Long> {

    enum Field implements OrderedField {
        TIMESTAMP
    }

    final class Default {

        private final CurrencyExchangeEventRepository repo;

        public Default(CurrencyExchangeEventRepository repo) {
            this.repo = repo;
        }

        public Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.streamExchangeEvents(pair.options, pair.limit);
        }

    }

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

    Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit);

    Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options); // default in java8

    Stream<CurrencyExchangeEvent> streamForDay(UtcDay day);

}
