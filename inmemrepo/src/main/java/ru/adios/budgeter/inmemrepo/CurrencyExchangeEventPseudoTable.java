package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:08 AM
 *
 * @author Mikhail Kulikov
 */
public final class CurrencyExchangeEventPseudoTable extends AbstractPseudoTable<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent> implements CurrencyExchangeEventRepository {

    public static final CurrencyExchangeEventPseudoTable INSTANCE = new CurrencyExchangeEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> table = new ConcurrentHashMap<>(100, 0.75f, 4);

    private CurrencyExchangeEventPseudoTable() {}

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        final int id = idSequence.incrementAndGet();
        checkState(table.putIfAbsent(id, new Stored<>(id, exchangeEvent)) == null);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> innerTable() {
        return table;
    }

    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.values().stream()
                .filter(stored -> new UtcDay(stored.obj.timestamp).equals(day))
                .map(currencyExchangeEventStored -> currencyExchangeEventStored.obj);
    }

}
