package ru.adios.budgeter.api;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 7/1/15
 * Time: 6:44 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class FundsMutationAgent {

    public static Builder builder() {
        return new Builder();
    }

    public final String name;

    private FundsMutationAgent(Builder builder) {
        name = builder.name;
        checkState(name != null, "name is null");
    }

    @Override
    public String toString() {
        return "FundsMutationAgent{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || !(o == null || getClass() != o.getClass())
                && name.equals(((FundsMutationAgent) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @NotThreadSafe
    public static final class Builder {

        private String name;

        private Builder() {}

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public FundsMutationAgent build() {
            return new FundsMutationAgent(this);
        }

    }

}
