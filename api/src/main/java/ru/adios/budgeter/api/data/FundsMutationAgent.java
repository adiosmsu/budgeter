package ru.adios.budgeter.api.data;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.OptionalLong;

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
        return builder().setAgent(agent).setId(id).build();
    }

    public final OptionalLong id;
    public final String name;
    public final String description;

    private FundsMutationAgent(Builder builder) {
        name = builder.name;
        id = builder.id;
        description = builder.description;
        checkState(name != null, "name is null");
    }

    @Override
    public String toString() {
        return "FundsMutationAgent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
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
        private String description;

        private Builder() {}

        public Builder setAgent(FundsMutationAgent agent) {
            this.id = agent.id;
            this.name = agent.name;
            this.description = agent.description;
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

        public String getDescription() {
            return description;
        }

        public FundsMutationAgent build() {
            return new FundsMutationAgent(this);
        }

    }

}
