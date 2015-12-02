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

package ru.adios.budgeter.api.data;

import java8.util.Optional;
import java8.util.OptionalLong;
import java8.util.function.Supplier;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 6/13/15
 * Time: 3:12 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class FundsMutationSubject {

    public static Builder builder(FundsMutationSubjectRepository repository) {
        return new Builder(repository);
    }

    private FundsMutationSubject(Builder builder) {
        builder.prepare();
        this.id = builder.id;
        this.parentId = builder.parentId;
        this.rootId = builder.rootId;
        this.childFlag = builder.childFlag;
        this.name = builder.name;
        this.repository = builder.repository;
        this.type = builder.type;
        this.description = builder.description;
    }

    public final OptionalLong id;
    public final long parentId;
    public final long rootId;
    public final boolean childFlag;
    public final Type type;
    public final String name;
    public final String description;

    private final FundsMutationSubjectRepository repository;

    public Optional<FundsMutationSubject> getParent() {
        return getInner(parentId);
    }

    public Optional<FundsMutationSubject> getRoot() {
        return getInner(rootId);
    }

    private Optional<FundsMutationSubject> getInner(long otherId) {
        return getInner(id, otherId, this, repository);
    }

    private static Optional<FundsMutationSubject> getInner(OptionalLong id, long otherId, FundsMutationSubject inst, FundsMutationSubjectRepository repository) {
        if (id.orElse(-1) == otherId)
            return Optional.ofNullable(inst);
        return repository.getById(otherId);
    }

    @Override
    public String toString() {
        return "FundsMutationSubject{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentId=" + parentId +
                ", rootId=" + rootId +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FundsMutationSubject that = (FundsMutationSubject) o;

        return parentId == that.parentId
                && rootId == that.rootId
                && type == that.type
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = (int) parentId;
        result = 31 * result + (int) rootId;
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public enum Type {
        PRODUCT,
        SERVICE,
        OCCASION
    }

    @NotThreadSafe
    public static final class Builder {

        private OptionalLong id = OptionalLong.empty();
        private long parentId;
        private long rootId;
        public boolean childFlag = false;
        private Type type;
        private String name;
        private String description;

        private final FundsMutationSubjectRepository repository;

        private Builder(FundsMutationSubjectRepository repository) {
            checkNotNull(repository);
            this.repository = repository;
        }

        public Builder setFundsMutationSubject(FundsMutationSubject subject) {
            id = subject.id;
            parentId = subject.parentId;
            rootId = subject.rootId;
            childFlag = subject.childFlag;
            type = subject.type;
            name = subject.name;
            description = subject.description;
            return this;
        }

        public Builder setId(long id) {
            checkArgument(id > 0);
            this.id = OptionalLong.of(id);
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setParentId(long parentId) {
            checkArgument(parentId >= 0, "Parent id must not be negative");
            if (parentId != this.parentId) {
                rootId = 0;
            }
            this.parentId = parentId;
            return this;
        }

        public Builder setRootId(long rootId) {
            checkArgument(rootId >= 0, "Root id must not be negative");
            this.rootId = rootId;
            return this;
        }

        public Builder setChildFlag(boolean childFlag) {
            this.childFlag = childFlag;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Type getType() {
            return type;
        }

        public long getParentId() {
            return parentId;
        }

        public FundsMutationSubject build() {
            return new FundsMutationSubject(this);
        }

        private void prepare() {
            checkArgument(id.orElse(1) > 0 && name != null && type != null, "Bad data, possibly uninitialized");
            if (parentId > 0 && rootId == 0) {
                final FundsMutationSubject parent = repository.getById(parentId).orElseThrow(new Supplier<IllegalStateException>() {
                    @Override
                    public IllegalStateException get() {
                        return new IllegalStateException("No database entry for parent [" + parentId + "] when searching root");
                    }
                });
                rootId = parent.rootId == 0 ? parentId : parent.rootId;
            }
        }

    }

    public static final String CUR_CONV_DIFF_NAME = "Разница при конвертации валют";
    public static final int CUR_CONV_DIFF_TYPE_ORDINAL = 2;

    public static FundsMutationSubject getCurrencyConversionDifferenceSubject(FundsMutationSubjectRepository repository) {
        final Optional<FundsMutationSubject> byId = repository.getById(repository.getIdForRateSubject());
        if (byId.isPresent()) {
            return byId.get();
        }
        return repository.addSubject(
                FundsMutationSubject.builder(repository)
                        .setId(repository.getIdForRateSubject())
                        .setName(CUR_CONV_DIFF_NAME)
                        .setType(Type.values()[CUR_CONV_DIFF_TYPE_ORDINAL])
                        .build()
        );
    }

}
