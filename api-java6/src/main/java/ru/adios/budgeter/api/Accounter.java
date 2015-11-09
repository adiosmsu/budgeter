package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableSet;
import java8.util.stream.Stream;
import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;

/**
 * Date: 6/12/15
 * Time: 7:26 PM
 *
 * @author Mikhail Kulikov
 */
public interface Accounter {

    FundsMutationEventRepository fundsMutationEventRepository();

    CurrencyExchangeEventRepository currencyExchangeEventRepository();

    PostponedFundsMutationEventRepository postponedFundsMutationEventRepository();

    PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository();


    Stream<PostponingReasons> streamAllPostponingReasons();

    Stream<PostponingReasons> streamAllPostponingReasons(boolean compatMode);

    FundsMutationSubjectRepository fundsMutationSubjectRepo();

    FundsMutationAgentRepository fundsMutationAgentRepo();

    @Immutable
    final class PostponingReasons {

        public final UtcDay dayUtc;
        public final ImmutableSet<CurrencyUnit> sufferingUnits;

        public PostponingReasons(UtcDay dayUtc, ImmutableSet<CurrencyUnit> sufferingUnits) {
            this.dayUtc = dayUtc;
            this.sufferingUnits = sufferingUnits;
        }

    }

}
