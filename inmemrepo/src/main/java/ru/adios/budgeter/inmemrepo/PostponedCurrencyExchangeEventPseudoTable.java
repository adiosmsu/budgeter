package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 10:52 AM
 *
 * @author Mikhail Kulikov
 */
public final class PostponedCurrencyExchangeEventPseudoTable
        extends AbstractPseudoTable<Stored<PostponedCurrencyExchangeEventRepository.PostponedExchange>, PostponedCurrencyExchangeEventRepository.PostponedExchange>
        implements PostponedCurrencyExchangeEventRepository
{

    public static final PostponedCurrencyExchangeEventPseudoTable INSTANCE = new PostponedCurrencyExchangeEventPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<PostponedCurrencyExchangeEventRepository.PostponedExchange>> table = new ConcurrentHashMap<>(100, 0.75f, 4);

    private PostponedCurrencyExchangeEventPseudoTable() {}

    @Override
    public void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp) {
        final int id = idSequence.incrementAndGet();
        checkState(
                table.computeIfAbsent(id, integer -> new Stored<>(id, new PostponedExchange(toBuy, unitSell, customRate, timestamp)))
                        .id == id
        );
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.values().stream().filter(event -> {
            final CurrencyUnit cu = event.obj.toBuy.getCurrencyUnit();
            return day.equals(new UtcDay(event.obj.timestamp))
                    && (cu.equals(oneOf) || cu.equals(secondOf))
                    && (event.obj.unitSell.equals(oneOf) || event.obj.unitSell.equals(secondOf));
        }).map(storedPostponedExchangeEvent -> storedPostponedExchangeEvent.obj);
    }

    Stream<PostponedExchange> streamAll() {
        return table.values().stream().map(storedPostponedExchangeEvent -> storedPostponedExchangeEvent.obj);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<PostponedCurrencyExchangeEventRepository.PostponedExchange>> innerTable() {
        return table;
    }

}
