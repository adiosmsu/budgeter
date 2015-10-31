package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
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
    public Optional<CurrencyExchangeEvent> getById(Long id) {
        final Stored<CurrencyExchangeEvent> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {
        final int id = idSequence.incrementAndGet();
        checkState(table.putIfAbsent(id, new Stored<>(id, exchangeEvent)) == null);
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        final int[] offsetCounter = new int[1], limitCounter = new int[1];
        offsetCounter[0] = 0; limitCounter[0] = 0;

        return table.values()
                .stream()
                .map(this::constructValid)
                .sorted((e1, e2) -> {
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
                })
                .filter(event1 ->
                                limit == null
                                        || !(limit.offset > 0 && limit.offset > offsetCounter[0]++)
                                        && !(limit.limit > 0 && limit.limit < ++limitCounter[0])
                );

    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<CurrencyExchangeEvent>> innerTable() {
        return table;
    }

    @Override
    public Stream<CurrencyExchangeEvent> streamForDay(UtcDay day) {
        return table.values().stream()
                .filter(stored -> new UtcDay(stored.obj.timestamp).equals(day))
                .map(this::constructValid);
    }

    private CurrencyExchangeEvent constructValid(Stored<CurrencyExchangeEvent> stored) {
        return CurrencyExchangeEvent.builder().setEvent(stored.obj)
                .setBoughtAccount(Schema.TREASURY.getAccountForName(stored.obj.boughtAccount.name).get())
                .setSoldAccount(Schema.TREASURY.getAccountForName(stored.obj.soldAccount.name).get())
                .build();
    }

}
