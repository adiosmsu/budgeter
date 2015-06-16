package ru.adios.budgeter;

import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Date: 6/13/15
 * Time: 8:57 PM
 *
 * @author Mikhail Kulikov
 */
public final class SubjectAdditionElementCore implements Submitter {

    private final FundsMutationSubject.Builder subjectBuilder;
    private final FundsMutationSubjectRepository repository;
    private Optional<FundsMutationSubject> parentRef = Optional.empty();

    public SubjectAdditionElementCore(FundsMutationSubjectRepository repository) {
        this.repository = repository;
        this.subjectBuilder = FundsMutationSubject.builder(repository);
    }

    public boolean setName(Optional<String> nameRef) {
        if (nameRef.isPresent()) {
            final String name = nameRef.get();
            if (name.length() > 0) {
                subjectBuilder.setName(name);
                return true;
            }
        }
        return false;
    }

    public boolean setParentName(String parentName) {
        parentRef = repository.findByName(parentName);
        if (parentRef.isPresent()) {
            final OptionalInt idParentRef = parentRef.get().id;
            if (idParentRef.isPresent()) {
                subjectBuilder.setParentId(idParentRef.getAsInt());
                return true;
            }
        }
        return false;
    }

    public void setType(int typeOrdinal) {
        subjectBuilder.setType(FundsMutationSubject.SubjectType.values()[typeOrdinal]);
    }

    @Override
    public void submit() {
        if (parentRef.isPresent()) {
            subjectBuilder.setRootId(parentRef.get().rootId);
        }
        repository.addSubject(subjectBuilder.build());
    }

}
