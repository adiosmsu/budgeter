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
        final Optional<FundsMutationSubject> parentRef = repository.findByName(parentName);
        if (parentRef.isPresent()) {
            final OptionalInt idParentRef = parentRef.get().id;
            if (idParentRef.isPresent()) {
                subjectBuilder.setParentId(idParentRef.getAsInt());
                return true;
            }
        }
        return false;
    }

    public boolean setType(int typeOrdinal) {
        final FundsMutationSubject.Type[] values = FundsMutationSubject.Type.values();
        if (typeOrdinal >= 0 && typeOrdinal < values.length) {
            subjectBuilder.setType(values[typeOrdinal]);
            return true;
        }
        return false;
    }

    public void setType(FundsMutationSubject.Type subjectType) {
        subjectBuilder.setType(subjectType);
    }

    @Override
    public void submit() {
        repository.addSubject(subjectBuilder.build());
    }

}
