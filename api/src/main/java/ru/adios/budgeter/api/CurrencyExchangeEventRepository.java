package ru.adios.budgeter.api;

import ru.adios.budgeter.api.data.CurrencyExchangeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

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

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

    Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit);

    Stream<CurrencyExchangeEvent> streamForDay(UtcDay day);

    default Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamExchangeEvents(pair.options, pair.limit);
    }

}
