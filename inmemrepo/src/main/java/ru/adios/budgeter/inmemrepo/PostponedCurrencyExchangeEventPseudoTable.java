package ru.adios.budgeter.inmemrepo;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.Treasury;
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
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public Optional<PostponedExchange> getById(Long id) {
        final Stored<PostponedExchange> stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.obj);
    }

    @Override
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          Treasury.BalanceAccount toBuyAccount,
                                          Treasury.BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent)
    {
        final int id = idSequence.incrementAndGet();
        checkState(
                table.computeIfAbsent(id, integer -> new Stored<>(id, new PostponedExchange(toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent)))
                        .id == id
        );
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.values().stream().filter(event -> {
            final CurrencyUnit bu = event.obj.toBuyAccount.getUnit();
            final CurrencyUnit su = event.obj.sellAccount.getUnit();
            return day.equals(new UtcDay(event.obj.timestamp))
                    && (bu.equals(oneOf) || bu.equals(secondOf))
                    && (su.equals(oneOf) || su.equals(secondOf));
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
