package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.PostponedCurrencyExchangeEventPseudoTable;

import java.math.BigDecimal;

/**
 * Date: 6/15/15
 * Time: 12:22 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventRepositoryMock implements PostponedCurrencyExchangeEventRepository {

    private final PostponedCurrencyExchangeEventPseudoTable table = PostponedCurrencyExchangeEventPseudoTable.INSTANCE;

    @Override
    public void rememberPostponedExchange(Money toBuy, CurrencyUnit unitSell, Optional<BigDecimal> customRate, OffsetDateTime timestamp, FundsMutationAgent agent) {
        table.rememberPostponedExchange(toBuy, unitSell, customRate, timestamp, agent);
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.streamRememberedExchanges(day, oneOf, secondOf);
    }

    public void clear() {
        table.clear();
    }

}
