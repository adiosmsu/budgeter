package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

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

    void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp);

    Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

    @Immutable
    final class PostponedExchange {

        public final Money toBuy;
        public final CurrencyUnit unitSell;
        public final Optional<BigDecimal> customRate;
        public final OffsetDateTime timestamp;

        public PostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp) {
            this.customRate = customRate.isPresent() ? Optional.of(customRate.get().stripTrailingZeros()) : Optional.empty();
            this.toBuy = toBuy;
            this.unitSell = unitSell;
            this.timestamp = timestamp;
        }

    }

}
