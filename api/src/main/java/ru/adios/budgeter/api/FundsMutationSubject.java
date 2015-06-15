package ru.adios.budgeter.api;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;

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
        this.childId = builder.childId;
        this.name = builder.name;
        this.repository = builder.repository;
        this.type = builder.type;
        checkArgument(id.orElse(1) > 0 && name != null && type != null, "Bad data, possibly uninitialized");
    }

    public final Optional<Integer> id;
    public final int parentId;
    public final int rootId;
    public final Optional<Integer> childId;
    public final SubjectType type;
    public final String name;

    private final FundsMutationSubjectRepository repository;

    public Optional<FundsMutationSubject> getParent() {
        return getInner(parentId);
    }

    public Optional<FundsMutationSubject> getRoot() {
        return getInner(rootId);
    }

    private Optional<FundsMutationSubject> getInner(int otherId) {
        if (id.orElse(-1) == otherId)
            return Optional.of(this);
        return repository.findById(otherId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FundsMutationSubject that = (FundsMutationSubject) o;
        return id.equals(that.id)
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public enum SubjectType {
        PRODUCT,
        SERVICE,
        OCCASION
    }

    @NotThreadSafe
    public static final class Builder {

        private Optional<Integer> id = Optional.empty();
        private int parentId;
        private int rootId;
        public Optional<Integer> childId = Optional.empty();
        private SubjectType type;
        private String name;

        private final FundsMutationSubjectRepository repository;

        private Builder(FundsMutationSubjectRepository repository) {
            checkNotNull(repository);
            this.repository = repository;
        }

        public Builder setId(int id) {
            this.id = Optional.of(id);
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(SubjectType type) {
            this.type = type;
            return this;
        }

        public Builder setParentId(int parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder setRootId(int rootId) {
            this.rootId = rootId;
            return this;
        }

        public Builder setChildId(int childId) {
            this.childId = Optional.of(childId);
            return this;
        }

        public FundsMutationSubject build() {
            return new FundsMutationSubject(this);
        }

        private void prepare() {
            if (parentId == 0)
                parentId = id.orElse(0);
            if (rootId == 0)
                rootId = id.orElse(0);
        }

    }

    public static final int CUR_CONV_DIFF_ID = 101;
    public static final String CUR_CONV_DIFF_NAME = "Разница при конвертации валют";
    public static final int CUR_CONV_DIFF_TYPE_ORDINAL = 2;

    public static FundsMutationSubject getCurrencyConversionDifference(FundsMutationSubjectRepository repository) {
        return FundsMutationSubject.builder(repository)
                .setId(CUR_CONV_DIFF_ID)
                .setName(CUR_CONV_DIFF_NAME)
                .setType(SubjectType.values()[CUR_CONV_DIFF_TYPE_ORDINAL])
                .build();
    }

}
