package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.CurrencyRatesProvider;
import ru.adios.budgeter.api.Treasury;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 4:21 PM
 *
 * @author Mikhail Kulikov
 */
public final class TreasuryPseudoTable extends AbstractPseudoTable<StoredBalanceAccount, Money> implements Treasury {

    public static final TreasuryPseudoTable INSTANCE = new TreasuryPseudoTable();
    private static final Logger logger = LoggerFactory.getLogger(TreasuryPseudoTable.class);

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, StoredBalanceAccount> table = new ConcurrentHashMap<Integer, StoredBalanceAccount>(15, 0.75f, 4);
    private final ConcurrentHashMap<CurrencyUnit, ImmutableList<Integer>> unitIndex = new ConcurrentHashMap<CurrencyUnit, ImmutableList<Integer>>(15, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<String, Integer>(15, 0.75f, 4);

    private final Default tDef = new Default(this);

    private TreasuryPseudoTable() {}

    @Override
    public Optional<BalanceAccount> getById(Long id) {
        final StoredBalanceAccount stored = table.get(id.intValue());
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(stored.createBalanceAccount());
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public void setSequenceValue(Long value) {
        idSequence.set(value.intValue());
    }

    @Override
    public Optional<Money> amount(CurrencyUnit unit) {
        final ImmutableList<Integer> idsList = unitIndex.get(unit);

        if (idsList == null || idsList.isEmpty()) {
            return Optional.empty();
        }

        Money sum = Money.zero(unit);
        for (final Integer id : idsList) {
            final Stored<Money> stored = table.get(id);
            checkNotNull(stored, "Record indexed but not stored (unit: %s, id: %s)", unit, id);
            sum = sum.plus(stored.obj);
        }
        return Optional.of(sum);
    }

    @Override
    public Money amountForHumans(CurrencyUnit unit) {
        return tDef.amountForHumans(unit);
    }

    @Override
    public Money totalAmount(CurrencyUnit unit, CurrencyRatesProvider ratesProvider) {
        return tDef.totalAmount(unit, ratesProvider);
    }

    @Override
    public Optional<Money> accountBalance(String accountName) {
        final Integer id = nameUniqueIndex.get(accountName);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(id).obj);
    }

    @Override
    public Optional<Money> accountBalance(BalanceAccount account) {
        return tDef.accountBalance(account);
    }

    @Override
    public void addAmount(Money amount, String accountName) {
        _addAmount(amount, accountName, false);
    }

    @Override
    public void addAmount(Money amount, BalanceAccount account) {
        tDef.addAmount(amount, account);
    }

    private BalanceAccount _addAmount(Money amount, String accountName, boolean createNew) {
        checkNotNull(amount, "amount is null");
        checkNotNull(accountName, "accountName is null");

        Integer id = nameUniqueIndex.get(accountName);
        if (id == null) {
            id = idSequence.incrementAndGet();
        } else {
            checkState(!createNew, "Not unique name %s", accountName);
        }

        final CurrencyUnit unit = amount.getCurrencyUnit();

        final long start = System.currentTimeMillis();
        final StoredBalanceAccount[] freshValueContainer = new StoredBalanceAccount[1];
        StoredBalanceAccount moneyStored;
        do {
            moneyStored = table.get(id);
            checkState(moneyStored == null || amount.getCurrencyUnit().equals(moneyStored.obj.getCurrencyUnit()),
                    "Account %s, trying to add %s", moneyStored, amount);

            freshValueContainer[0] = (moneyStored != null)
                    ? new StoredBalanceAccount(id, moneyStored.obj.plus(amount), accountName)
                    : new StoredBalanceAccount(id, amount, accountName);

            checkState(System.currentTimeMillis() - start < 5000, "Row insert/update timeout");
        } while (moneyStored != null
                ? replaceStoredFailed(id, moneyStored, freshValueContainer)
                : insertNewStoredFailed(accountName, id, unit, start, freshValueContainer, createNew));

        return freshValueContainer[0].createBalanceAccount();
    }

    private boolean replaceStoredFailed(Integer id, StoredBalanceAccount moneyStored, Object[] fresh) {
        final StoredBalanceAccount newValue = (StoredBalanceAccount) fresh[0];

        final boolean success = table.replace(id, moneyStored, newValue);
        if (logger.isDebugEnabled()) {
            if (success) {
                logger.debug("Replaced stored balance account {} with {}", moneyStored, newValue);
            } else {
                logger.debug("Was too late to replace of stored balance account {} with {}", moneyStored, newValue);
            }
        }
        return !success;
    }

    private boolean insertNewStoredFailed(final String accountName, final Integer id, final CurrencyUnit unit, final long start, final Object[] fresh, final boolean createNew) {
        final StoredBalanceAccount insertedValue = table.computeIfAbsent(id, new Function<Integer, StoredBalanceAccount>() {
            @Override
            public StoredBalanceAccount apply(Integer key) {
                final StoredBalanceAccount toStore = (StoredBalanceAccount) fresh[0];
                if (nameUniqueIndex.putIfAbsent(accountName, key) != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Not unique name {} for {}", accountName, toStore);
                    }
                    checkState(!createNew, "Not unique name %s for %s", accountName, toStore);
                    return null;
                }

                ImmutableList<Integer> idsForUnit;
                ImmutableList.Builder<Integer> builder;
                do {
                    idsForUnit = unitIndex.get(unit);
                    builder = ImmutableList.builder();
                    if (idsForUnit != null) {
                        for (final Integer idFu : idsForUnit) {
                            builder.add(idFu);
                        }
                    }
                    builder.add(key);
                    checkState(System.currentTimeMillis() - start < 5000, "Row insert/update timeout");
                } while (idsForUnit != null
                        ? !unitIndex.replace(unit, idsForUnit, builder.build())
                        : unitIndex.putIfAbsent(unit, builder.build()) != null);

                return toStore;
            }
        });

        final boolean success =
                insertedValue != null && insertedValue.equals(fresh[0]);
        if (logger.isDebugEnabled()) {
            final StoredBalanceAccount toStore = (StoredBalanceAccount) fresh[0];
            if (success) {
                logger.debug("Inserted stored balance account " + toStore);
            } else {
                logger.debug("Was too late to store balance account " + toStore);
            }
        }
        return !success;
    }

    @Override
    public BalanceAccount registerBalanceAccount(BalanceAccount account) {
        return _addAmount(Money.zero(account.getUnit()), account.name, true);
    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        return StreamSupport.stream(unitIndex.keySet());
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(final CurrencyUnit unit) {
        return StreamSupport.stream(unitIndex.get(unit)).map(new Function<Integer, BalanceAccount>() {
            @Override
            public BalanceAccount apply(Integer id) {
                final StoredBalanceAccount stored = table.get(id);
                checkNotNull(stored, "Indexed unit %s not stored for id %s", unit, id);
                return stored.createBalanceAccount(id.longValue());
            }
        });
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return StreamSupport.stream(table.values()).map(new Function<StoredBalanceAccount, BalanceAccount>() {
            @Override
            public BalanceAccount apply(StoredBalanceAccount stored) {
                return stored.createBalanceAccount();
            }
        });
    }

    @Override
    public BalanceAccount getAccountWithId(BalanceAccount account) {
        if (account.id != null) {
            return account;
        }
        final Integer key = nameUniqueIndex.get(account.name);
        return new BalanceAccount(key.longValue(), account.name, accountBalance(account.name).get());
    }

    @Override
    public Optional<BalanceAccount> getAccountForName(String accountName) {
        final Integer key = nameUniqueIndex.get(accountName);
        if (key == null) {
            return Optional.empty();
        }
        return Optional.of(new BalanceAccount(key.longValue(), accountName, accountBalance(accountName).get()));
    }

    @Nonnull
    @Override
    ConcurrentHashMap<Integer, StoredBalanceAccount> innerTable() {
        return table;
    }

    @Override
    public void clear() {
        table.clear();
        unitIndex.clear();
        nameUniqueIndex.clear();
    }

}
