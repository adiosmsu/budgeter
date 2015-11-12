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

package ru.adios.budgeter.api;

import ru.adios.budgeter.api.data.FundsMutationAgent;

import static org.junit.Assert.assertEquals;

/**
 * Date: 10/26/15
 * Time: 4:41 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationAgentRepoTester {

    private final Bundle bundle;

    public FundsMutationAgentRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testAddAgent() throws Exception {
        bundle.clear(Bundle.Repo.FUNDS_MUTATION_AGENTS);
        final FundsMutationAgentRepository agentRepository = bundle.fundsMutationAgents();
        final FundsMutationAgent agent = FundsMutationAgent.builder().setName("Test").build();
        agentRepository.addAgent(agent);
        assertEquals("Test", agentRepository.getById(agentRepository.currentSeqValue()).get().name);
        assertEquals(agent, agentRepository.findByName("Test").get());
    }

}
