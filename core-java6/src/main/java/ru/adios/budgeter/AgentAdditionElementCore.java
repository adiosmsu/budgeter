package ru.adios.budgeter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationAgentRepository;

import javax.annotation.Nullable;

/**
 * Date: 10/16/15
 * Time: 6:23 PM
 *
 * @author Mikhail Kulikov
 */
public class AgentAdditionElementCore implements Submitter<FundsMutationAgent> {

    public static final String FIELD_NAME = "name";

    private static final Logger logger = LoggerFactory.getLogger(AgentAdditionElementCore.class);


    private final FundsMutationAgentRepository repository;
    private final FundsMutationAgent.Builder agentBuilder = FundsMutationAgent.builder();

    private boolean lockOn = false;
    private Result<FundsMutationAgent> storedResult;

    public AgentAdditionElementCore(FundsMutationAgentRepository repository) {
        this.repository = repository;
    }

    public void setName(String name) {
        if (lockOn) return;
        agentBuilder.setName(name);
    }

    @Nullable
    public String getName() {
        return agentBuilder.getName();
    }

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

        try {
            return Result.success(repository.addAgent(agentBuilder.build()));
        } catch (RuntimeException ex) {
            logger.error("Error while adding new agent", ex);
            return resultBuilder
                    .setGeneralError("Error while adding new agent: " + ex.getMessage())
                    .build();
        }
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

    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

}
