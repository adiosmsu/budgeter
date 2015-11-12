/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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

    private final ConcurrentHashMap<Integer, StoredBalanceAccount> table = new ConcurrentHashMap<>(15, 0.75f, 4);
    private final ConcurrentHashMap<CurrencyUnit, ImmutableList<Integer>> unitIndex = new ConcurrentHashMap<>(15, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<>(15, 0.75f, 4);

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
    public Optional<Money> accountBalance(String accountName) {
        final Integer id = nameUniqueIndex.get(accountName);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(id).obj);
    }

    @Override
    public void addAmount(Money amount, String accountName) {
        _addAmount(amount, accountName, null, false);
    }

    private BalanceAccount _addAmount(Money amount, String accountName, String desc, boolean createNew) {
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
                    ? new StoredBalanceAccount(id, moneyStored.obj.plus(amount), accountName, desc)
                    : new StoredBalanceAccount(id, amount, accountName, desc);

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

    private boolean insertNewStoredFailed(String accountName, Integer id, CurrencyUnit unit, long start, Object[] fresh, boolean createNew) {
        final StoredBalanceAccount insertedValue = table.computeIfAbsent(id, key -> {
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
                    idsForUnit.forEach(builder::add);
                }
                builder.add(key);
                checkState(System.currentTimeMillis() - start < 5000, "Row insert/update timeout");
            } while (idsForUnit != null
                    ? !unitIndex.replace(unit, idsForUnit, builder.build())
                    : unitIndex.putIfAbsent(unit, builder.build()) != null);

            return toStore;
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
        return _addAmount(Money.zero(account.getUnit()), account.name, account.description.orElse(null), true);
    }

    @Override
    public Stream<CurrencyUnit> streamRegisteredCurrencies() {
        return unitIndex.keySet().stream();
    }

    @Override
    public Stream<BalanceAccount> streamAccountsByCurrency(CurrencyUnit unit) {
        return unitIndex.get(unit).stream().map(id -> {
            final StoredBalanceAccount stored = table.get(id);
            checkNotNull(stored, "Indexed unit %s not stored for id %s", unit, id);
            return stored.createBalanceAccount(id.longValue());
        });
    }

    @Override
    public Stream<BalanceAccount> streamRegisteredAccounts() {
        return table.values().stream().map(StoredBalanceAccount::createBalanceAccount);
    }

    @Override
    public BalanceAccount getAccountWithId(BalanceAccount account) {
        if (account.id.isPresent()) {
            return account;
        }
        final Integer key = nameUniqueIndex.get(account.name);
        final StoredBalanceAccount storedBalanceAccount = table.get(key);
        return new BalanceAccount(key.longValue(), account.name, storedBalanceAccount.desc, storedBalanceAccount.obj);
    }

    @Override
    public Optional<BalanceAccount> getAccountForName(String accountName) {
        final Integer key = nameUniqueIndex.get(accountName);
        if (key == null) {
            return Optional.empty();
        }
        final StoredBalanceAccount storedBalanceAccount = table.get(key);
        return Optional.of(new BalanceAccount(key.longValue(), accountName, storedBalanceAccount.desc, storedBalanceAccount.obj));
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
