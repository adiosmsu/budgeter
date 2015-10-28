package ru.adios.budgeter.api;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 7/1/15
 * Time: 6:48 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationAgentRepository extends Provider<FundsMutationAgent, Long> {

    FundsMutationAgent addAgent(FundsMutationAgent agent);

    Stream<FundsMutationAgent> streamAll();

    Optional<FundsMutationAgent> findByName(String name);

    FundsMutationAgent getAgentWithId(FundsMutationAgent agent);

}
