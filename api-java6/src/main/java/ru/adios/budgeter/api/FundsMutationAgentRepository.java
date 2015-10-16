package ru.adios.budgeter.api;

import java8.util.Optional;

/**
 * Date: 7/1/15
 * Time: 6:48 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationAgentRepository {

    FundsMutationAgent addAgent(FundsMutationAgent agent);

    Optional<FundsMutationAgent> findByName(String name);

}
