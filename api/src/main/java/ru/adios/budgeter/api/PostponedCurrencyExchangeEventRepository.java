package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.PostponedExchange;

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
public interface PostponedCurrencyExchangeEventRepository extends Provider<PostponedExchange, Long> {

    void rememberPostponedExchange(
            BigDecimal toBuy,
            BalanceAccount toBuyAccount,
            BalanceAccount sellAccount,
            Optional<BigDecimal> customRate,
            OffsetDateTime timestamp,
            FundsMutationAgent agent
    );

    Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf);

}
