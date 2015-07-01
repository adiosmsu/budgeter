package ru.adios.budgeter;

import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.CurrencyExchangeEventPseudoTable;

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
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        table.registerCurrencyExchange(exchangeEvent);
    }

    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.streamForDay(day);
    }

    public void clear() {
        table.clear();
    }

}
