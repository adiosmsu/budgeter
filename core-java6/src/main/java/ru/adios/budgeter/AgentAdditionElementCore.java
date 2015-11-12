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

import java8.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationAgentRepository;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.data.FundsMutationAgent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Date: 10/16/15
 * Time: 6:23 PM
 *
 * @author Mikhail Kulikov
 */
public class AgentAdditionElementCore implements Submitter<FundsMutationAgent> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";

    private static final Logger logger = LoggerFactory.getLogger(AgentAdditionElementCore.class);


    private final FundsMutationAgentRepository repository;
    private final FundsMutationAgent.Builder agentBuilder = FundsMutationAgent.builder();
    private final SubmitHelper<FundsMutationAgent> helper = new SubmitHelper<FundsMutationAgent>(logger, "Error while adding new agent");

    private boolean lockOn = false;
    private Result<FundsMutationAgent> storedResult;

    public AgentAdditionElementCore(FundsMutationAgentRepository repository) {
        this.repository = repository;
    }

    public void setName(String name) {
        if (lockOn) return;
        agentBuilder.setName(name);
    }

    public void setDescription(String description) {
        if (lockOn) return;
        agentBuilder.setDescription(description);
    }

    @Nullable
    public String getName() {
        return agentBuilder.getName();
    }

    @Nullable
    public String getDescription() {
        return agentBuilder.getDescription();
    }

    @PotentiallyBlocking
    @Override
    public Result<FundsMutationAgent> submit() {
        ResultBuilder<FundsMutationAgent> resultBuilder = new ResultBuilder<FundsMutationAgent>();
        final String name = getName();
        resultBuilder.addFieldErrorIfNull(name, FIELD_NAME);
        if (name != null && name.equals("")) {
            resultBuilder.addFieldError(FIELD_NAME);
        }
        if (resultBuilder.toBuildError())
            return resultBuilder.build();

        return helper.doSubmit(new Supplier<Result<FundsMutationAgent>>() {
            @Override
            public Result<FundsMutationAgent> get() {
                return doSubmit();
            }
        }, resultBuilder);
    }

    @Nonnull
    private Result<FundsMutationAgent> doSubmit() {
        return Result.success(repository.addAgent(agentBuilder.build()));
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
    public Result<FundsMutationAgent> getStoredResult() {
        return storedResult;
    }

    @PotentiallyBlocking
    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
