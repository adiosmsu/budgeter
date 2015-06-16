package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 7:17 PM
 *
 * @author Mikhail Kulikov
 */
public final class CurrencyRatesPseudoTable extends AbstractPseudoTable<StoredCurrencyRate, UtcDay> implements CurrencyRatesRepository {

    public static final CurrencyRatesPseudoTable INSTANCE = new CurrencyRatesPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, StoredCurrencyRate> table = new ConcurrentHashMap<>(100, 0.75f, 4);
    private final ConcurrentHashMap<UtcDay, ImmutableSet<Integer>> dayIndex = new ConcurrentHashMap<>(100, 0.75f, 4);

    private CurrencyRatesPseudoTable() {}

    @Override
    public void addRate(final UtcDay dayUtc, final CurrencyUnit from, final CurrencyUnit to, final BigDecimal rate) {
        final int id = idSequence.incrementAndGet();
        table.computeIfAbsent(id, key -> {
            final long start = System.currentTimeMillis();
            ImmutableSet<Integer> ids;
            ImmutableSet<Integer> fresh;
            do {
                ids = dayIndex.get(dayUtc);
                final ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
                builder.add(key);
                if (ids != null) {
                    ids.stream().forEach(integer -> {
                        final StoredCurrencyRate storedCurrencyRate = table.get(integer);
                        checkState((!storedCurrencyRate.first.equals(from) || !storedCurrencyRate.second.equals(to))
                                && (!storedCurrencyRate.first.equals(to) || !storedCurrencyRate.second.equals(from)), "Such rate already present (%s,%s,%s)", dayUtc, from, to);
                    });
                    builder.addAll(ids);
                }
                fresh = builder.build();
                checkState(System.currentTimeMillis() - start < 5000, "dayIndex update timeout");
            } while (ids != null ? !dayIndex.replace(dayUtc, ids, fresh) : dayIndex.putIfAbsent(dayUtc, fresh) != null);

            return new StoredCurrencyRate(key, dayUtc, from, to, rate);
        });
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return dayIndex.get(day)
                .stream()
                .map(table::get)
                .map(rate -> getRelevance(from, to, rate))
                .filter(relevance -> relevance.actualRate.isPresent())
                .map(relevance -> relevance.actualRate.get())
                .findFirst();
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        final BigDecimal[] res = new BigDecimal[1];
        dayIndex.keySet().stream().sorted((o1, o2) -> o2.compareTo(o1)).filter(day ->
                dayIndex.get(day).stream().filter(
                        id -> {
                            final StoredCurrencyRate rate = table.get(id);
                            final Relevance relevance = getRelevance(from, to, rate);
                            final boolean found = relevance.actualRate.isPresent();
                            if (found) {
                                res[0] = relevance.actualRate.get();
                            }
                            return found;
                        }
                ).findFirst().isPresent()).findFirst();
        return Optional.ofNullable(res[0]);
    }

    @Override
    public boolean isRateStale(CurrencyUnit to) {
        return !dayIndex.get(new UtcDay()).stream().anyMatch(id -> {
            final StoredCurrencyRate rate = table.get(id);
            return rate.first.equals(to) || rate.second.equals(to);
        });
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, StoredCurrencyRate> innerTable() {
        return table;
    }

    ConcurrentHashMap<UtcDay, ImmutableSet<Integer>> getDayIndex() {
        return dayIndex;
    }

    @Override
    public void clear() {
        table.clear();
        dayIndex.clear();
    }

    private static Relevance getRelevance(CurrencyUnit from, CurrencyUnit to, StoredCurrencyRate storedCurrencyRate) {
        if (storedCurrencyRate.first.equals(from) && storedCurrencyRate.second.equals(to)) {
            return new Relevance(storedCurrencyRate.rate);
        } else if (storedCurrencyRate.first.equals(to) && storedCurrencyRate.second.equals(from)) {
            return new Relevance(CurrencyRatesProvider.reverseRate(storedCurrencyRate.rate));
        }

        return new Relevance(null);
    }

    private static final class Relevance {
        private final Optional<BigDecimal> actualRate;

        private Relevance(BigDecimal rate) {
            actualRate = Optional.ofNullable(rate);
        }
    }

}
