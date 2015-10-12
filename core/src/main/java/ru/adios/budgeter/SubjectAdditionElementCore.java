package ru.adios.budgeter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Date: 6/13/15
 * Time: 8:57 PM
 *
 * @author Mikhail Kulikov
 */
public final class SubjectAdditionElementCore implements Submitter<FundsMutationSubject> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_PARENT_NAME = "parentName";

    private static final Logger logger = LoggerFactory.getLogger(SubjectAdditionElementCore.class);

    private final FundsMutationSubject.Builder subjectBuilder;
    private final FundsMutationSubjectRepository repository;

    private String parentName;

    public SubjectAdditionElementCore(FundsMutationSubjectRepository repository) {
        this.repository = repository;
        this.subjectBuilder = FundsMutationSubject.builder(repository);
    }

    public void setName(String name) {
        subjectBuilder.setName(name);
    }

    @Nullable
    public String getName() {
        return subjectBuilder.getName();
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
        final Optional<FundsMutationSubject> parentRef = repository.findByName(parentName);
        if (parentRef.isPresent()) {
            final OptionalInt idParentRef = parentRef.get().id;
            if (idParentRef.isPresent()) {
                subjectBuilder.setParentId(idParentRef.getAsInt());
            }
        }
    }

    @Nullable
    public String getParentName() {
        return parentName;
    }

    public void setType(int typeOrdinal) {
        final FundsMutationSubject.Type[] values = FundsMutationSubject.Type.values();
        if (typeOrdinal >= 0 && typeOrdinal < values.length) {
            subjectBuilder.setType(values[typeOrdinal]);
        }
    }

    public void setType(FundsMutationSubject.Type subjectType) {
        subjectBuilder.setType(subjectType);
    }

    @Nullable
    public FundsMutationSubject.Type getType() {
        return subjectBuilder.getType();
    }

    @Override
    public Result<FundsMutationSubject> submit() {
        final Submitter.ResultBuilder<FundsMutationSubject> resultBuilder = new ResultBuilder<>();
        final String name = subjectBuilder.getName();
        resultBuilder.addFieldErrorIfNull(name, FIELD_NAME)
                .addFieldErrorIfNull(subjectBuilder.getType(), FIELD_TYPE);
        if (name != null && name.equals("")) {
            resultBuilder.addFieldError(FIELD_NAME);
        }
        if (parentName != null && subjectBuilder.getParentId() == 0) {
            resultBuilder.addFieldError(FIELD_PARENT_NAME);
            parentName = null;
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        try {
            return Result.success(repository.addSubject(subjectBuilder.build()));
        } catch (RuntimeException ex) {
            logger.error("Error while adding new subject", ex);
            return resultBuilder
                    .setGeneralError("Error while adding new subject: " + ex.getMessage())
                    .build();
        }
    }

}
