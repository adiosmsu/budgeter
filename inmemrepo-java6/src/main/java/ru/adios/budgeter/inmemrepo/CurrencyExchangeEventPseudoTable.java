package ru.adios.budgeter.inmemrepo;

import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import ru.adios.budgeter.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
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
    private final Default def = new Default(this);

    private CurrencyExchangeEventPseudoTable() {}

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        final int id = idSequence.incrementAndGet();
        checkState(table.putIfAbsent(id, new Stored<CurrencyExchangeEvent>(id, exchangeEvent)) == null);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(final List<OrderBy<Field>> options, @Nullable final OptLimit limit) {
        final int[] offsetCounter = new int[1], limitCounter = new int[1];
        offsetCounter[0] = 0; limitCounter[0] = 0;

        return StreamSupport.stream(table.values().getSpliterator(), false)
                .map(new Function<Stored<CurrencyExchangeEvent>, CurrencyExchangeEvent>() {
                    @Override
                    public CurrencyExchangeEvent apply(Stored<CurrencyExchangeEvent> stored) {
                        return stored.obj;
                    }
                })
                .sorted(new Comparator<CurrencyExchangeEvent>() {
                    @Override
                    public int compare(CurrencyExchangeEvent e1, CurrencyExchangeEvent e2) {
                        int res = 1;
                        for (final OrderBy<Field> opt : options) {
                            switch (opt.field) {
                                case TIMESTAMP:
                                    res = opt.order.applyToCompareResult(e1.timestamp.compareTo(e2.timestamp));
                                    if (res < 0) {
                                        return -1;
                                    }
                                    break;
                            }
                        }
                        return res;
                    }
                })
                .filter(new Predicate<CurrencyExchangeEvent>() {
                    @Override
                    public boolean test(CurrencyExchangeEvent event) {
                        return limit == null
                                || !(limit.offset > 0 && limit.offset > offsetCounter[0]++)
                                && !(limit.limit > 0 && limit.limit < ++limitCounter[0]);
                    }
                });
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(RepoOption... options) {
        return def.stream(options);
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
