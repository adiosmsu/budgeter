package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.Spliterators;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.CurrencyRatesRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.ConversionRate;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
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

    private final ConcurrentHashMap<Integer, StoredCurrencyRate> table = new ConcurrentHashMap<Integer, StoredCurrencyRate>(100, 0.75f, 4);
    private final ConcurrentHashMap<UtcDay, ImmutableSet<Integer>> dayIndex = new ConcurrentHashMap<UtcDay, ImmutableSet<Integer>>(100, 0.75f, 4);

    private final CurrencyRatesRepository.Default repoDef = new CurrencyRatesRepository.Default(this);

    private CurrencyRatesPseudoTable() {}

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public Optional<ConversionRate> getById(Long id) {
        final StoredCurrencyRate stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.createConversionRate());
    }

    @Override
    public boolean addRate(final UtcDay dayUtc, final CurrencyUnit from, final CurrencyUnit to, final BigDecimal rate) {
        final Object[] mutableFlag = new Object[] {this};
        PseudoTable.Static.nonUniqueIndexedInsert(
                table,
                dayIndex,
                idSequence.incrementAndGet(),
                new Function<Integer, StoredCurrencyRate>() {
                    @Override
                    public StoredCurrencyRate apply(Integer id) {
                        return new StoredCurrencyRate(id, dayUtc, from, to, rate);
                    }
                },
                Optional.of(dayUtc),
                Optional.<Consumer<StoredCurrencyRate>>of(new Consumer<StoredCurrencyRate>() {
                    @Override
                    public void accept(StoredCurrencyRate scr) {
                        if (scr.first.equals(from) && scr.second.equals(to)) {
                            mutableFlag[0] = null;
                        }
                    }
                })
        );
        return mutableFlag[0] != null;
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, final CurrencyUnit from, final CurrencyUnit to) {
        final ImmutableSet<Integer> indexed = dayIndex.get(day);
        if (indexed == null)
            return Optional.empty();
        return StreamSupport.stream(Spliterators.spliterator(indexed, 0), false)
                .map(new Function<Integer, StoredCurrencyRate>() {
                    @Override
                    public StoredCurrencyRate apply(Integer key) {
                        return table.get(key);
                    }
                })
                .filter(new Predicate<StoredCurrencyRate>() {
                    @Override
                    public boolean test(StoredCurrencyRate rate) {
                        return rate.first.equals(from) && rate.second.equals(to);
                    }
                })
                .map(new Function<StoredCurrencyRate, BigDecimal>() {
                    @Override
                    public BigDecimal apply(StoredCurrencyRate rate) {
                        return rate.rate;
                    }
                })
                .findFirst();
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(final CurrencyUnit from, final CurrencyUnit to) {
        return StreamSupport.stream(dayIndex.entrySet().getSpliterator(), false)
                .sorted(new Comparator<Map.Entry<UtcDay, ImmutableSet<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<UtcDay, ImmutableSet<Integer>> entry1, Map.Entry<UtcDay, ImmutableSet<Integer>> entry2) {
                        return entry2.getKey().compareTo(entry1.getKey());
                    }
                })
                .map(new Function<Map.Entry<UtcDay,ImmutableSet<Integer>>, Optional<BigDecimal>>() {
                    @Override
                    public Optional<BigDecimal> apply(Map.Entry<UtcDay, ImmutableSet<Integer>> entry) {
                        for (final Integer id : entry.getValue()) {
                            StoredCurrencyRate rate = table.get(id);
                            if (rate.first.equals(from) && rate.second.equals(to)) {
                                return Optional.of(rate.rate);
                            }
                        }
                        return Optional.empty();
                    }
                })
                .filter(new Predicate<Optional<BigDecimal>>() {
                    @Override
                    public boolean test(Optional<BigDecimal> bigDecimalOptional) {
                        return bigDecimalOptional.isPresent();
                    }
                })
                .findFirst()
                .orElse(Optional.<BigDecimal>empty());
    }

    @Override
    public boolean isRateStale(final CurrencyUnit to) {
        return !StreamSupport.stream(dayIndex.get(new UtcDay())).anyMatch(new Predicate<Integer>() {
            @Override
            public boolean test(Integer id) {
                final StoredCurrencyRate rate = table.get(id);
                return rate.first.equals(to) || rate.second.equals(to);
            }
        });
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getConversionMultiplier(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierBidirectional(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getConversionMultiplierBidirectional(day, from, to);
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return repoDef.getConversionMultiplierWithIntermediate(day, from, to, intermediate);
    }

    @Override
    public BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getLatestConversionMultiplier(from, to);
    }

    @Override
    public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        return repoDef.getLatestConversionMultiplierWithIntermediate(from, to, intermediate);
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        return repoDef.getLatestOptionalConversionMultiplierBidirectional(from, to);
    }

    @Override
    public boolean addTodayRate(CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        return repoDef.addTodayRate(from, to, rate);
    }

    @Override
    public ImmutableSet<Long> getIndexedForDay(UtcDay day) {
        final ImmutableSet<Integer> integers = dayIndex.get(day);
        final ImmutableSet.Builder<Long> longsBuilder = ImmutableSet.builder();
        for (final Integer i : integers) {
            longsBuilder.add(i.longValue());
        }
        return longsBuilder.build();
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
