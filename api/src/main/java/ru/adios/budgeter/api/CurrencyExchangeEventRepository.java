package ru.adios.budgeter.api;

/**
 * Date: 6/15/15
 * Time: 9:52 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyExchangeEventRepository {

    void registerCurrencyExchange(CurrencyExchangeEvent exchangeEvent);

}
