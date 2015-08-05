package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.BalancesRepository;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 4:21 PM
 *
 * @author Mikhail Kulikov
 */
public final class TreasuryPseudoTable extends AbstractPseudoTable<Stored<Money>, Money> implements Treasury {

    public static final TreasuryPseudoTable INSTANCE = new TreasuryPseudoTable();

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, Stored<Money>> table = new ConcurrentHashMap<Integer, Stored<Money>>(15, 0.75f, 4);
    private final ConcurrentHashMap<CurrencyUnit, Integer> unitUniqueIndex = new ConcurrentHashMap<CurrencyUnit, Integer>(15, 0.75f, 4);

    private final BalancesRepository.Default brDef = new BalancesRepository.Default(this);
    private final Treasury.Default tDef = new Treasury.Default(this);

    private TreasuryPseudoTable() {}

    @SuppressWarnings("unchecked")
    @Override
    public void addAmount(Money amount) {
        final CurrencyUnit unit = amount.getCurrencyUnit();
        Integer id = unitUniqueIndex.get(unit);
        if (id == null) {
            id = idSequence.incrementAndGet();
        }
        final long start = System.currentTimeMillis();
        Stored<Money> moneyStored;
        final Object[] fresh = new Object[1];
        //noinspection EqualsBetweenInconvertibleTypes
        do {
            moneyStored = table.get(id);
            fresh[0] = moneyStored != null
                    ? new Stored<Money>(id, moneyStored.obj.plus(amount))
                    : new Stored<Money>(id, amount);
            checkState(System.currentTimeMillis() - start < 5000, "Row insert/update timeout");
        } while (moneyStored != null ? !table.replace(id, moneyStored, (Stored<Money>) fresh[0]) : !table.computeIfAbsent(id, new Function<Integer, Stored<Money>>() {
            @Override
            public Stored<Money> apply(Integer key) {
                checkState(unitUniqueIndex.putIfAbsent(unit, key) == null, "Not unique unit %s", unit);
                return (Stored<Money>) fresh[0];
            }
        }).equals(fresh[0]));
    }

    @Override
    public Stream<CurrencyUnit> getRegisteredCurrencies() {
        return StreamSupport.stream(table.values().getSpliterator(), false).map(new Function<Stored<Money>, CurrencyUnit>() {
            @Override
            public CurrencyUnit apply(Stored<Money> moneyStored) {
                return moneyStored.obj.getCurrencyUnit();
            }
        });
    }

    @Override
    public void registerCurrency(CurrencyUnit unit) {
        final int id = idSequence.incrementAndGet();
        checkState(unitUniqueIndex.computeIfAbsent(unit, new Function<CurrencyUnit, Integer>() {
            @Override
            public Integer apply(CurrencyUnit unit) {
                table.put(id, new Stored<Money>(id, Money.zero(unit)));
                return id;
            }
        }).equals(id), "Not unique unit %s", unit);
    }

    @Override
    public ImmutableList<CurrencyUnit> searchCurrenciesByString(final String str) {
        final List<CurrencyUnit> collected =
                StreamSupport.stream(unitUniqueIndex.keySet().getSpliterator(), false)
                        .filter(new Predicate<CurrencyUnit>() {
                            @Override
                            public boolean test(CurrencyUnit unit) {
                                return unit.getCode().toUpperCase().startsWith(str.toUpperCase());
                            }
                        })
                        .collect(Collectors.<CurrencyUnit>toList());
        return ImmutableList.copyOf(collected);
    }

    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        final Integer i = unitUniqueIndex.get(unit);
        if (i == null)
            return Optional.empty();
        return Optional.ofNullable(table.get(i).obj);
    }

    @Override
    public Money amountForHumans(CurrencyUnit unit) {
        return brDef.amountForHumans(unit);
    }

    @Override
    public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return tDef.totalAmount(unit, ratesProvider);
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, Stored<Money>> innerTable() {
        return table;
    }

    @Override
    public void clear() {
        table.clear();
        unitUniqueIndex.clear();
    }

}
