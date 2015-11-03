package ru.adios.budgeter.api.data;

import java8.util.OptionalLong;

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

    public static FundsMutationAgent withId(FundsMutationAgent agent, long id) {
        return builder().setId(id).setName(agent.name).build();
    }

    public final OptionalLong id;
    public final String name;

    private FundsMutationAgent(Builder builder) {
        name = builder.name;
        id = builder.id;
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

        private OptionalLong id = OptionalLong.empty();
        private String name;

        private Builder() {}

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setId(Long id) {
            if (id != null) {
                this.id = OptionalLong.of(id);
            } else {
                this.id = OptionalLong.empty();
            }
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
