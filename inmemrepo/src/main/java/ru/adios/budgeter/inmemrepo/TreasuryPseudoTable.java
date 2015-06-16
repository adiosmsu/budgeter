package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final ConcurrentHashMap<Integer, Stored<Money>> table = new ConcurrentHashMap<>(15, 0.75f, 4);
    private final ConcurrentHashMap<CurrencyUnit, Integer> unitUniqueIndex = new ConcurrentHashMap<>(15, 0.75f, 4);

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
                    ? new Stored<>(id, moneyStored.obj.plus(amount))
                    : new Stored<>(id, amount);
            checkState(System.currentTimeMillis() - start < 5000, "Row insert/update timeout");
        } while (moneyStored != null ? !table.replace(id, moneyStored, (Stored<Money>) fresh[0]) : !table.computeIfAbsent(id, key -> {
            checkState(unitUniqueIndex.putIfAbsent(unit, key) == null, "Not unique unit %s", unit);
            return (Stored<Money>) fresh[0];
        }).equals(fresh[0]));
    }

    @Override
    public Stream<CurrencyUnit> getRegisteredCurrencies() {
        return table.values().stream().map(moneyStored -> moneyStored.obj.getCurrencyUnit());
    }

    @Override
    public void registerCurrency(CurrencyUnit unit) {
        final int id = idSequence.incrementAndGet();
        checkState(unitUniqueIndex.computeIfAbsent(unit, key -> {
            table.put(id, new Stored<>(id, Money.zero(unit)));
            return id;
        }).equals(id), "Not unique unit %s", unit);
    }

    @Override
    public ImmutableList<CurrencyUnit> searchCurrenciesByString(String str) {
        final List<CurrencyUnit> collected =
                unitUniqueIndex.keySet()
                        .stream()
                        .filter(unit -> unit.getCode().toUpperCase().startsWith(str.toUpperCase()))
                        .collect(Collectors.toList());
        return ImmutableList.copyOf(collected);
    }

    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        final Integer i = unitUniqueIndex.get(unit);
        if (i == null)
            return Optional.empty();
        return Optional.ofNullable(table.get(i).obj);
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
