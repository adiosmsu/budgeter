package ru.adios.budgeter.inmemrepo;

import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ConcurrentHashMap<Integer, Stored<PostponedCurrencyExchangeEventRepository.PostponedExchange>> table = new ConcurrentHashMap<Integer, Stored<PostponedExchange>>(100, 0.75f, 4);

    private PostponedCurrencyExchangeEventPseudoTable() {}

    @Override
    public void rememberPostponedExchange(final BigDecimal toBuy,
                                          final Treasury.BalanceAccount toBuyAccount,
                                          final Treasury.BalanceAccount sellAccount,
                                          final Optional<BigDecimal> customRate,
                                          final OffsetDateTime timestamp,
                                          final FundsMutationAgent agent)
    {
        final int id = idSequence.incrementAndGet();
        checkState(
                table.computeIfAbsent(id, new Function<Integer, Stored<PostponedExchange>>() {
                    @Override
                    public Stored<PostponedExchange> apply(Integer integer) {
                        return new Stored<PostponedExchange>(id, new PostponedExchange(toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent));
                    }
                }).id == id
        );
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(final UtcDay day, final CurrencyUnit oneOf, final CurrencyUnit secondOf) {
        return StreamSupport.stream(table.values().getSpliterator(), false)
                .filter(new Predicate<Stored<PostponedExchange>>() {
                    @Override
                    public boolean test(Stored<PostponedExchange> event) {
                        final CurrencyUnit bu = event.obj.toBuyAccount.getUnit();
                        final CurrencyUnit su = event.obj.sellAccount.getUnit();
                        return day.equals(new UtcDay(event.obj.timestamp))
                                && (bu.equals(oneOf) || bu.equals(secondOf))
                                && (su.equals(oneOf) || su.equals(secondOf));
                    }
                })
                .map(new Function<Stored<PostponedExchange>, PostponedExchange>() {
                    @Override
                    public PostponedExchange apply(Stored<PostponedExchange> postponedExchangeStored) {
                        return postponedExchangeStored.obj;
                    }
                });
    }

    Stream<PostponedExchange> streamAll() {
        return StreamSupport.stream(table.values().getSpliterator(), false).map(new Function<Stored<PostponedExchange>, PostponedExchange>() {
            @Override
            public PostponedExchange apply(Stored<PostponedExchange> postponedExchangeStored) {
                return postponedExchangeStored.obj;
            }
        });
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<PostponedCurrencyExchangeEventRepository.PostponedExchange>> innerTable() {
        return table;
    }

}
