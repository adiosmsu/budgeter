package ru.adios.budgeter.jdbcrepo;

import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 10/26/15
 * Time: 8:20 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrencyExchangeEventJdbcRepository implements CurrencyExchangeEventRepository {

    public static final String TABLE_NAME = "currency_exchange_event";
    public static final String SEQ_NAME = "seq_currency_exchange_event";


    private final SafeJdbcTemplateProvider jdbcTemplateProvider;
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;

    CurrencyExchangeEventJdbcRepository(SafeJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }


    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }


    @Override
    public void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent) {

    }

    @Override
    public Stream<CurrencyExchangeEvent> streamExchangeEvents(List<OrderBy<Field>> options, @Nullable OptLimit limit) {
        return null;
    }

    @Override
    public Optional<CurrencyExchangeEvent> getById(Long id) {
        return null;
    }

    @Override
    public Long currentSeqValue() {
        return null;
    }

    private String getActualCreateTableSql() {
        return ""; //TODO: impl
    }

    String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                // TODO: indexes
        };
    }


}
