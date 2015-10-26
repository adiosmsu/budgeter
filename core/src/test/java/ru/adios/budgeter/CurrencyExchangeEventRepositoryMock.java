package ru.adios.budgeter;

import ru.adios.budgeter.api.*;
import ru.adios.budgeter.inmemrepo.CurrencyExchangeEventPseudoTable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.streamForDay(day);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return table.streamExchangeEvents(options, limit);
    }

    public void clear() {
        table.clear();
    }

}
