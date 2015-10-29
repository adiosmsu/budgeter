package ru.adios.budgeter.jdbcrepo;

import org.intellij.lang.annotations.Language;
import ru.adios.budgeter.api.*;

import java.util.stream.Stream;

/**
 * Date: 10/29/15
 * Time: 5:29 AM
 *
 * @author Mikhail Kulikov
 */
public class JdbcAccounter implements Accounter {

    @Language("SQL")
    private static final String EXCHANGES_SQL = "";
    @Language("SQL")
    private static final String MUTATIONS_SQL = "";

    private final SourcingBundle bundle;

    JdbcAccounter(SourcingBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public CurrencyExchangeEventRepository currencyExchangeEventRepository() {
        return bundle.currencyExchangeEvents();
    }

    @Override
    public FundsMutationEventRepository fundsMutationEventRepository() {
        return bundle.fundsMutationEvents();
    }

    @Override
    public PostponedFundsMutationEventRepository postponedFundsMutationEventRepository() {
        return bundle.postponedFundsMutationEvents();
    }

    @Override
    public PostponedCurrencyExchangeEventRepository postponedCurrencyExchangeEventRepository() {
        return bundle.postponedCurrencyExchangeEvents();
    }

    @Override
    public FundsMutationSubjectRepository fundsMutationSubjectRepo() {
        return bundle.fundsMutationSubjects();
    }

    @Override
    public FundsMutationAgentRepository fundsMutationAgentRepo() {
        return bundle.fundsMutationAgents();
    }

    @Override
    public Stream<PostponingReasons> streamAllPostponingReasons() {
        return null;
    }

}
