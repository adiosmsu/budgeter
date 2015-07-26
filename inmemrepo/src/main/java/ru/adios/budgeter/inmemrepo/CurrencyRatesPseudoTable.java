package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    public boolean addRate(final UtcDay dayUtc, final CurrencyUnit from, final CurrencyUnit to, final BigDecimal rate) {
        final Object[] mutableFlag = new Object[] {this};
        PseudoTable.nonUniqueIndexedInsert(
                table,
                dayIndex,
                idSequence.incrementAndGet(),
                id -> new StoredCurrencyRate(id, dayUtc, from, to, rate),
                Optional.of(dayUtc),
                Optional.of(scr -> { if (scr.first.equals(from) && scr.second.equals(to)) mutableFlag[0] = null; })
        );
        return mutableFlag[0] != null;
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        final ImmutableSet<Integer> indexed = dayIndex.get(day);
        if (indexed == null)
            return Optional.empty();
        return indexed.stream()
                .map(table::get)
                .filter(rate -> rate.first.equals(from) && rate.second.equals(to))
                .map(rate -> rate.rate)
                .findFirst();
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return dayIndex.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getKey().compareTo(entry1.getKey()))
                .map(entry -> {
                    for (final Integer id : entry.getValue()) {
                        StoredCurrencyRate rate = table.get(id);
                        if (rate.first.equals(from) && rate.second.equals(to)) {
                            return Optional.of(rate.rate);
                        }
                    }
                    return Optional.<BigDecimal>empty();
                })
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.<BigDecimal>empty());
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

}
