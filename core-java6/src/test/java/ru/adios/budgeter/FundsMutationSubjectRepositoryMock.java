package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.inmemrepo.FundsMutationSubjectPseudoTable;

/**
 * Date: 6/15/15
 * Time: 9:01 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectRepositoryMock implements FundsMutationSubjectRepository {

    private final FundsMutationSubjectPseudoTable pseudoTable = FundsMutationSubjectPseudoTable.INSTANCE;
    private final FundsMutationSubjectRepository.Default fmsRepoDef = new Default(this);

    @Override
    public Optional<FundsMutationSubject> getById(Long id) {
        return pseudoTable.getById(id);
    }

    @Override
    public Long currentSeqValue() {
        return pseudoTable.currentSeqValue();
    }

    @Override
    public Optional<FundsMutationSubject> findByName(String name) {
        return pseudoTable.findByName(name);
    }

    @Override
    public Stream<FundsMutationSubject> findByParent(long parentId) {
        return pseudoTable.findByParent(parentId);
    }

    @Override
    public Stream<FundsMutationSubject> streamAll() {
        return pseudoTable.streamAll();
    }

    @Override
    public ImmutableList<FundsMutationSubject> nameLikeSearch(String str) {
        return pseudoTable.nameLikeSearch(str);
    }

    @Override
    public long getIdForRateSubject() {
        return pseudoTable.getIdForRateSubject();
    }

    @Override
    public FundsMutationSubject rawAddition(FundsMutationSubject subject) {
        return pseudoTable.rawAddition(subject);
    }

    @Override
    public void updateChildFlag(long id) {
        pseudoTable.updateChildFlag(id);
    }

    @Override
    public FundsMutationSubject addSubject(FundsMutationSubject subject) {
        return fmsRepoDef.addSubject(subject);
    }

    public void clear() {
        pseudoTable.clear();
    }

}
