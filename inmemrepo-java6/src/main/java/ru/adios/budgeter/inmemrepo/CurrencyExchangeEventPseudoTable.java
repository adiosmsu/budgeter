package ru.adios.budgeter.inmemrepo;

import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> table = new ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>>(100, 0.75f, 4);

    private CurrencyExchangeEventPseudoTable() {}

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        final int id = idSequence.incrementAndGet();
        checkState(table.putIfAbsent(id, new Stored<CurrencyExchangeEvent>(id, exchangeEvent)) == null);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> innerTable() {
        return table;
    }

    public Stream<CurrencyExchangeEvent> streamForDay(final UtcDay day) {
        return StreamSupport.stream(table.values().getSpliterator(), false)
                .filter(new Predicate<Stored<CurrencyExchangeEvent>>() {
                    @Override
                    public boolean test(Stored<CurrencyExchangeEvent> stored) {
                        return new UtcDay(stored.obj.timestamp).equals(day);
                    }
                })
                .map(new Function<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent>() {
                    @Override
                    public CurrencyExchangeEvent apply(Stored<CurrencyExchangeEvent> stored) {
                        return stored.obj;
                    }
                });
    }

}
