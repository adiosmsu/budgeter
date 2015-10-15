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
    public Optional<FundsMutationSubject> findById(int id) {
        return pseudoTable.findById(id);
    }

    @Override
    public Optional<FundsMutationSubject> findByName(String name) {
        return pseudoTable.findByName(name);
    }

    @Override
    public Stream<FundsMutationSubject> findByParent(int parentId) {
        return pseudoTable.findByParent(parentId);
    }

    @Override
    public ImmutableList<FundsMutationSubject> nameLikeSearch(String str) {
        return pseudoTable.nameLikeSearch(str);
    }

    @Override
    public int idSeqNext() {
        return pseudoTable.idSeqNext();
    }

    @Override
    public int getIdForRateSubject() {
        return pseudoTable.getIdForRateSubject();
    }

    @Override
    public void rawAddition(FundsMutationSubject subject) {
        pseudoTable.rawAddition(subject);
    }

    @Override
    public void updateChildFlag(int id) {
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
