package ru.adios.budgeter.api;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 9:55 AM
 *
 * @author Mikhail Kulikov
 */
public interface PostponedCurrencyExchangeEventRepository {

    void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp, FundsMutationAgent agent);

    Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    @Immutable
    final class PostponedExchange {

        public final Money toBuy;
        public final CurrencyUnit unitSell;
        public final Optional<BigDecimal> customRate;
        public final OffsetDateTime timestamp;
        public final FundsMutationAgent agent;

        public PostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp, FundsMutationAgent agent) {
            this.agent = agent;
            this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.<BigDecimal>empty();
            this.toBuy = toBuy;
            this.unitSell = unitSell;
            this.timestamp = timestamp;
        }

    }

}
