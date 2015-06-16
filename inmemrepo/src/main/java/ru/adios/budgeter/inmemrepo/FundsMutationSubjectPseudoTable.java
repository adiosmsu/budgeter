package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
        INSTANCE.addSubject(FundsMutationSubject.getCurrencyConversionDifference(INSTANCE));
    }

    final AtomicInteger idSequence = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, FundsMutationSubject> table = new ConcurrentHashMap<>(100, 0.75f, 4);
    private final ConcurrentHashMap<String, Integer> nameUniqueIndex = new ConcurrentHashMap<>(100, 0.75f, 4);
    private final ConcurrentHashMap<Integer, ImmutableSet<Integer>> parentIndex = new ConcurrentHashMap<>(100, 0.75f, 4);

    private FundsMutationSubjectPseudoTable() {}

    @Override
    public int idSeqNext() {
        return idSequence.incrementAndGet();
    }

    @Override
    public void rawAddition(final FundsMutationSubject subject) {
        if (subject.id.isPresent()) {
            final int idConcrete = subject.id.getAsInt();
            checkState(PseudoTable.nonUniqueIndexedInsert(
                    table,
                    parentIndex,
                    idConcrete,
                    key -> subject,
                    Optional.ofNullable(subject.parentId > 0 ? subject.parentId : null),
                    Optional.empty(),
                    o -> checkState(nameUniqueIndex.putIfAbsent(subject.name, (Integer) o) == null, "Name %s already occupied", subject.name)
            ).equals(subject), "Id %s occupied", idConcrete);
        } else {
            rawAddition(FundsMutationSubject.builder(this)
                    .setFundsMutationSubject(subject)
                    .setId(idSequence.incrementAndGet())
                    .build());
        }

    }

    @Override
    public void updateChildFlag(int id) {
        final long start = System.currentTimeMillis();
        FundsMutationSubject inter;
        while (!table.replace(id, inter = table.get(id), FundsMutationSubject.builder(this).setFundsMutationSubject(inter).setChildFlag(true).build())) {
            checkState(System.currentTimeMillis() - start < 10000, "Record unbelievably busy: unable to set child flag to record with id %s", id);
        }
    }

    @Override
    public Optional<FundsMutationSubject> findById(int id) {
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public Optional<FundsMutationSubject> findByName(String name) {
        return Optional.ofNullable(table.get(nameUniqueIndex.get(name)));
    }

    @Override
    public Stream<FundsMutationSubject> findByParent(int parentId) {
        return parentIndex.get(parentId).stream().map(table::get);
    }

    @Override
    public ImmutableList<FundsMutationSubject> searchByString(String str) {
        final ImmutableList.Builder<FundsMutationSubject> builder = ImmutableList.builder();
        table.values().stream().forEach(subject -> {
            if (subject.name.startsWith(str))
                builder.add(subject);
        });
        return builder.build();
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
    }

}
