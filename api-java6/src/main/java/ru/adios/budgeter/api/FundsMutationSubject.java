package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Supplier;

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
    }

    public final OptionalInt id;
    public final int parentId;
    public final int rootId;
    public final boolean childFlag;
    public final Type type;
    public final String name;

    private final FundsMutationSubjectRepository repository;

    public Optional<FundsMutationSubject> getParent() {
        return getInner(parentId);
    }

    public Optional<FundsMutationSubject> getRoot() {
        return getInner(rootId);
    }

    private Optional<FundsMutationSubject> getInner(int otherId) {
        return getInner(id, otherId, this, repository);
    }

    private static Optional<FundsMutationSubject> getInner(OptionalInt id, int otherId, FundsMutationSubject inst, FundsMutationSubjectRepository repository) {
        if (id.orElse(-1) == otherId)
            return Optional.ofNullable(inst);
        return repository.findById(otherId);
    }

    @Override
    public String toString() {
        return "FundsMutationSubject{" +
                "name='" + name + '\'' +
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
        int result = parentId;
        result = 31 * result + rootId;
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

        private OptionalInt id = OptionalInt.empty();
        private int parentId;
        private int rootId;
        public boolean childFlag = false;
        private Type type;
        private String name;

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
            return this;
        }

        public Builder setId(int id) {
            checkArgument(id > 0);
            this.id = OptionalInt.of(id);
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setParentId(int parentId) {
            checkArgument(parentId > 0);
            this.parentId = parentId;
            return this;
        }

        public Builder setRootId(int rootId) {
            checkArgument(rootId > 0);
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

        public Type getType() {
            return type;
        }

        public int getParentId() {
            return parentId;
        }

        public FundsMutationSubject build() {
            return new FundsMutationSubject(this);
        }

        private void prepare() {
            checkArgument(id.orElse(1) > 0 && name != null && type != null, "Bad data, possibly uninitialized");
            if (parentId > 0 && rootId == 0) {
                final FundsMutationSubject parent = repository.findById(parentId).orElseThrow(new Supplier<IllegalStateException>() {
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
        return FundsMutationSubject.builder(repository)
                .setId(repository.getIdForRateSubject())
                .setName(CUR_CONV_DIFF_NAME)
                .setType(Type.values()[CUR_CONV_DIFF_TYPE_ORDINAL])
                .build();
    }

}
