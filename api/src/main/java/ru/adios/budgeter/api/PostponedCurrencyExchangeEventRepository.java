package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 9:55 AM
 *
 * @author Mikhail Kulikov
 */
public interface PostponedCurrencyExchangeEventRepository {

    void rememberPostponedExchange(
            BigDecimal toBuy,
            Treasury.BalanceAccount toBuyAccount,
            Treasury.BalanceAccount sellAccount,
            Optional<BigDecimal> customRate,
            OffsetDateTime timestamp,
            FundsMutationAgent agent
    );

    Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    @Immutable
    final class PostponedExchange {

        public final BigDecimal toBuy;
        public final Treasury.BalanceAccount toBuyAccount;
        public final Treasury.BalanceAccount sellAccount;
        public final Optional<BigDecimal> customRate;
        public final OffsetDateTime timestamp;
        public final FundsMutationAgent agent;

        public PostponedExchange(
                BigDecimal toBuy,
                Treasury.BalanceAccount toBuyAccount,
                Treasury.BalanceAccount sellAccount,
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

}
