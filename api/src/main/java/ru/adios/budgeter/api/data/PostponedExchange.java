package ru.adios.budgeter.api.data;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Date: 11/3/15
 * Time: 10:56 AM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public final class PostponedExchange {

    public final BigDecimal toBuy;
    public final BalanceAccount toBuyAccount;
    public final BalanceAccount sellAccount;
    public final Optional<BigDecimal> customRate;
    public final OffsetDateTime timestamp;
    public final FundsMutationAgent agent;

    public PostponedExchange(
            BigDecimal toBuy,
            BalanceAccount toBuyAccount,
            BalanceAccount sellAccount,
            Optional<BigDecimal> customRate,
            OffsetDateTime timestamp,
            FundsMutationAgent agent
    ) {
        this.agent = agent;
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.empty();
        this.toBuy = toBuy;
        this.toBuyAccount = toBuyAccount;
        this.sellAccount = sellAccount;
        this.timestamp = timestamp;
    }

}
