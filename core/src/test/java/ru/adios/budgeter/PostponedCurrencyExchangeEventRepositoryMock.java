package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.PostponedCurrencyExchangeEventRepository;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.PostponedCurrencyExchangeEventPseudoTable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/15/15
 * Time: 12:22 PM
 *
 * @author Mikhail Kulikov
 */
public class PostponedCurrencyExchangeEventRepositoryMock implements PostponedCurrencyExchangeEventRepository {

    private final PostponedCurrencyExchangeEventPseudoTable table = PostponedCurrencyExchangeEventPseudoTable.INSTANCE;

    @Override
    public Optional<PostponedExchange> getById(Long id) {
        return table.getById(id);
    }

    @Override
    public Long currentSeqValue() {
        return table.currentSeqValue();
    }

    @Override
    public void rememberPostponedExchange(BigDecimal toBuy,
                                          Treasury.BalanceAccount toBuyAccount,
                                          Treasury.BalanceAccount sellAccount,
                                          Optional<BigDecimal> customRate,
                                          OffsetDateTime timestamp,
                                          FundsMutationAgent agent)
    {
        table.rememberPostponedExchange(toBuy, toBuyAccount, sellAccount, customRate, timestamp, agent);
    }

    @Override
    public Stream<PostponedExchange> streamRememberedExchanges(UtcDay day, CurrencyUnit oneOf, CurrencyUnit secondOf) {
        return table.streamRememberedExchanges(day, oneOf, secondOf);
    }

    public void clear() {
        table.clear();
    }

}
