package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import java8.util.concurrent.ConcurrentHashMap;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/15/15
 * Time: 9:02 AM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationSubjectPseudoTable implements FundsMutationSubjectRepository, PseudoTable<FundsMutationSubject> {

    public static final FundsMutationSubjectPseudoTable INSTANCE;

    static {
        INSTANCE = new FundsMutationSubjectPseudoTable();
        INSTANCE.addSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(INSTANCE));
    }

    final AtomicInteger idSequence = new AtomicInteger(1);

    private final ConcurrentHashMap<Integer, FundsMutationSubject> table = new ConcurrentHashMap<Integer, FundsMutationSubject>(100, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<String, Integer>(100, 0.75f, 4);
    private final ConcurrentHashMap<Integer, ImmutableSet<Integer>> parentIndex = new ConcurrentHashMap<Integer, ImmutableSet<Integer>>(100, 0.75f, 4);

    private final FundsMutationSubjectRepository.Default fmsRepoDef = new Default(this);

    private FundsMutationSubjectPseudoTable() {}

    @Override
    public Optional<FundsMutationSubject> getById(Long id) {
        return Optional.ofNullable(table.get(id.intValue()));
    }

    @Override
    public Long currentSeqValue() {
        return (long) idSequence.get();
    }

    @Override
    public long getIdForRateSubject() {
        return fmsRepoDef.getIdForRateSubject();
    }

    @Override
    public FundsMutationSubject rawAddition(final FundsMutationSubject subject) {
        if (subject.id.isPresent()) {
            final int idConcrete = (int) subject.id.getAsLong();
            checkState(PseudoTable.Static.nonUniqueIndexedInsert(
                    table,
                    parentIndex,
                    idConcrete,
                    new Function<Integer, FundsMutationSubject>() {
                        @Override
                        public FundsMutationSubject apply(Integer integer) {
                            return subject;
                        }
                    },
                    Optional.ofNullable(subject.parentId > 0 ? (int) subject.parentId : null),
                    Optional.<Consumer<FundsMutationSubject>>empty(),
                    new Consumer() {
                        @Override
                        public void accept(Object o) {
                            checkState(nameUniqueIndex.putIfAbsent(subject.name, (Integer) o) == null, "Name %s already occupied", subject.name);
                        }
                    }
            ).equals(subject), "Id %s occupied", idConcrete);
            return subject;
        } else {
            return rawAddition(FundsMutationSubject.builder(this)
                    .setFundsMutationSubject(subject)
                    .setId(idSequence.incrementAndGet())
                    .build());
        }
    }

    @Override
    public void updateChildFlag(long id) {
        final long start = System.currentTimeMillis();
        FundsMutationSubject inter;
        while (!table.replace((int) id, inter = table.get((int) id), FundsMutationSubject.builder(this).setFundsMutationSubject(inter).setChildFlag(true).build())) {
            checkState(System.currentTimeMillis() - start < 10000, "Record unbelievably busy: unable to set child flag to record with id %s", id);
        }
    }

    @Override
    public Optional<FundsMutationSubject> findByName(String name) {
        final Integer id = nameUniqueIndex.get(name);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public Stream<FundsMutationSubject> findByParent(long parentId) {
        return StreamSupport.stream(parentIndex.get((int) parentId)).map(new Function<Integer, FundsMutationSubject>() {
            @Override
            public FundsMutationSubject apply(Integer integer) {
                return table.get(integer);
            }
        });
    }

    @Override
    public Stream<FundsMutationSubject> streamAll() {
        return StreamSupport.stream(table.values().getSpliterator(), false);
    }

    @Override
    public ImmutableList<FundsMutationSubject> nameLikeSearch(final String str) {
        final ImmutableList.Builder<FundsMutationSubject> builder = ImmutableList.builder();
        StreamSupport.stream(table.values().getSpliterator(), false).forEach(new Consumer<FundsMutationSubject>() {
            @Override
            public void accept(FundsMutationSubject subject) {
                final int length = str.length();
                if (length > 0) {
                    if (str.charAt(0) == '%') {
                        if (str.charAt(length - 1) == '%') {
                            if (length > 2) {
                                if (subject.name.contains(str.substring(1, length - 1))) {
                                    builder.add(subject);
                                }
                            } else {
                                builder.add(subject);
                            }
                        } else if (subject.name.endsWith(str.substring(1))) {
                            builder.add(subject);
                        }
                    } else if (str.charAt(length - 1) == '%') {
                        if (subject.name.startsWith(str.substring(0, length - 1))) {
                            builder.add(subject);
                        }
                    } else if (str.equals(subject.name)) {
                        builder.add(subject);
                    }
                }
            }
        });
        return builder.build();
    }

    @Override
    public FundsMutationSubject addSubject(FundsMutationSubject subject) {
        return fmsRepoDef.addSubject(subject);
    }

    @Override
    public FundsMutationSubject get(int id) {
        return table.get(id);
    }

    @Override
    public void clear() {
        table.clear();
        nameUniqueIndex.clear();
        parentIndex.clear();
        addSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(INSTANCE));
    }

}
