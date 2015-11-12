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

package ru.adios.budgeter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalLong;

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
    public static final String FIELD_DESCRIPTION = "description";

    private static final Logger logger = LoggerFactory.getLogger(SubjectAdditionElementCore.class);


    private final FundsMutationSubject.Builder subjectBuilder;
    private final FundsMutationSubjectRepository repository;
    private final SubmitHelper<FundsMutationSubject> helper = new SubmitHelper<>(logger, "Error while adding new subject");

    private volatile String parentName;

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

    public void setDescription(String description) {
        if (lockOn) return;
        subjectBuilder.setDescription(description);
    }

    @Nullable
    public String getName() {
        return subjectBuilder.getName();
    }

    @Nullable
    public String getDescription() {
        return subjectBuilder.getDescription();
    }

    @PotentiallyBlocking
    public Optional<FundsMutationSubject> setParentName(String parentName) {
        return setParentName(parentName, false);
    }

    @PotentiallyBlocking
    public Optional<FundsMutationSubject> setParentName(String parentName, boolean fromSeparateThread) {
        if (lockOn) return Optional.empty();

        this.parentName = parentName;

        if (parentName == null) {
            if (!fromSeparateThread) {
                subjectBuilder.setParentId(0);
            }
            return Optional.empty();
        }

        final Optional<FundsMutationSubject> parentRef = repository.findByName(parentName);

        if (parentRef.isPresent()) {
            final OptionalLong idParentRef = parentRef.get().id;
            if (idParentRef.isPresent()) {
                if (!fromSeparateThread) {
                    subjectBuilder.setParentId(idParentRef.getAsLong());
                }
                return parentRef;
            }
        }

        return Optional.empty();
    }

    public void setParentId(long parentId) {
        subjectBuilder.setParentId(parentId);
    }

    public long getParentId() {
        return subjectBuilder.getParentId();
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
        resultBuilder.addFieldErrorIfShorter(subjectBuilder.getName(), 3, FIELD_NAME)
                .addFieldErrorIfNull(subjectBuilder.getType(), FIELD_TYPE);
        if (parentName != null && subjectBuilder.getParentId() == 0) {
            resultBuilder.addFieldError(FIELD_PARENT_NAME);
            parentName = null;
        }

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        return helper.doSubmit(this::doSubmit, resultBuilder);
    }

    @Nonnull
    private Result<FundsMutationSubject> doSubmit() {
        return Result.success(repository.addSubject(subjectBuilder.build()));
    }

    @Override
    public void setTransactional(TransactionalSupport transactional) {
        helper.setTransactionalSupport(transactional);
    }

    @Override
    public TransactionalSupport getTransactional() {
        return helper.getTransactionalSupport();
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
