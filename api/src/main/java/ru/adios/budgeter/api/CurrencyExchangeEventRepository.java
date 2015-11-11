package ru.adios.budgeter.api;

import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

    int countExchangeEvents();

    Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<CurrencyExchangeEvent> streamForDay(UtcDay day);

    default Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamExchangeEvents(pair.options, pair.limit);
    }

}
