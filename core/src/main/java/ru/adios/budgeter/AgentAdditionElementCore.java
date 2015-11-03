package ru.adios.budgeter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationAgentRepository;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.data.FundsMutationAgent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Date: 10/16/15
 * Time: 6:33 PM
 *
 * @author Mikhail Kulikov
 */
public class AgentAdditionElementCore implements Submitter<FundsMutationAgent> {

    public static final String FIELD_NAME = "name";

    private static final Logger logger = LoggerFactory.getLogger(AgentAdditionElementCore.class);


    private final FundsMutationAgentRepository repository;
    private final FundsMutationAgent.Builder agentBuilder = FundsMutationAgent.builder();
    private final SubmitHelper<FundsMutationAgent> helper = new SubmitHelper<>(logger, "Error while adding new agent");

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

    @PotentiallyBlocking
    @Override
    public Result<FundsMutationAgent> submit() {
        ResultBuilder<FundsMutationAgent> resultBuilder = new ResultBuilder<>();
        final String name = getName();
        resultBuilder.addFieldErrorIfNull(name, FIELD_NAME);
        if (name != null && name.equals("")) {
            resultBuilder.addFieldError(FIELD_NAME);
        }
        if (resultBuilder.toBuildError())
            return resultBuilder.build();

        return helper.doSubmit(this::doSubmit, resultBuilder);
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
