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

    private boolean lockOn = false;
    private Result<FundsMutationSubject> storedResult;

    public SubjectAdditionElementCore(FundsMutationSubjectRepository repository) {
        this.repository = repository;
        this.subjectBuilder = FundsMutationSubject.builder(repository);
    }

    public void setName(String name) {
        if (lockOn) return;
        subjectBuilder.setName(name);
    }

    @Nullable
    public String getName() {
        return subjectBuilder.getName();
    }

    @PotentiallyBlocking
    public boolean setParentName(String parentName) {
        if (lockOn) return false;

        this.parentName = parentName;

        if (parentName == null) {
            subjectBuilder.setParentId(0);
            return true;
        }

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

    @Nullable
    public String getParentName() {
        return parentName;
    }

    public void setType(int typeOrdinal) {
        if (lockOn) return;
        final FundsMutationSubject.Type[] values = FundsMutationSubject.Type.values();
        if (typeOrdinal >= 0 && typeOrdinal < values.length) {
            subjectBuilder.setType(values[typeOrdinal]);
        }
    }

    public void setType(FundsMutationSubject.Type subjectType) {
        if (lockOn) return;
        subjectBuilder.setType(subjectType);
    }

    @Nullable
    public FundsMutationSubject.Type getType() {
        return subjectBuilder.getType();
    }

    @PotentiallyBlocking
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

    @Override
    public void lock() {
        lockOn = true;
    }

    @Override
    public void unlock() {
        lockOn = false;
    }

    @Override
    public Result<FundsMutationSubject> getStoredResult() {
        return storedResult;
    }

    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
