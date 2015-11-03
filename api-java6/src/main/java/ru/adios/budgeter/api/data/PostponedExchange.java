package ru.adios.budgeter.api.data;

import java8.util.Optional;
import org.threeten.bp.OffsetDateTime;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 11/3/15
 * Time: 11:01 AM
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
        this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.<BigDecimal>empty();
        this.toBuy = toBuy;
        this.toBuyAccount = toBuyAccount;
        this.sellAccount = sellAccount;
        this.timestamp = timestamp;
    }

}
