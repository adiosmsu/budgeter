package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.CurrencyExchangeEventPseudoTable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Date: 6/15/15
 * Time: 10:08 AM
 *
 * @author Mikhail Kulikov
 */
public class CurrencyExchangeEventRepositoryMock implements CurrencyExchangeEventRepository {

    private final CurrencyExchangeEventPseudoTable table = CurrencyExchangeEventPseudoTable.INSTANCE;

    @Override
    public Long currentSeqValue() {
        return table.currentSeqValue();
    }

    @Override
    public Optional<CurrencyExchangeEvent> getById(Long id) {
        return table.getById(id);
    }

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        table.registerCurrencyExchange(exchangeEvent);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return table.streamExchangeEvents(options, limit);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        return table.streamExchangeEvents(options);
    }

    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.streamForDay(day);
    }

    public void clear() {
        table.clear();
    }

}
