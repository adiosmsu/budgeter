package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.threeten.bp.OffsetDateTime;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 9:55 AM
 *
 * @author Mikhail Kulikov
 */
public interface PostponedCurrencyExchangeEventRepository extends Provider<PostponedCurrencyExchangeEventRepository.PostponedExchange, Long> {

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
            this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.<BigDecimal>empty();
            this.toBuy = toBuy;
            this.toBuyAccount = toBuyAccount;
            this.sellAccount = sellAccount;
            this.timestamp = timestamp;
        }

    }

}
