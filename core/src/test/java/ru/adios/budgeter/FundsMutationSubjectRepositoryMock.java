package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.inmemrepo.FundsMutationSubjectPseudoTable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:01 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectRepositoryMock implements FundsMutationSubjectRepository {

    private final FundsMutationSubjectPseudoTable pseudoTable = FundsMutationSubjectPseudoTable.INSTANCE;

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

    public void clear() {
        pseudoTable.clear();
    }

}
