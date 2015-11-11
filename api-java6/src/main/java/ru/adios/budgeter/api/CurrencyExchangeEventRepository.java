package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Date: 6/15/15
 * Time: 9:52 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyExchangeEventRepository extends Provider<CurrencyExchangeEvent, Long> {

    enum Field implements OrderedField, Serializable {
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

    int countExchangeEvents();

    Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options); // default in java8

    Stream<CurrencyExchangeEvent> streamForDay(UtcDay day);

}
