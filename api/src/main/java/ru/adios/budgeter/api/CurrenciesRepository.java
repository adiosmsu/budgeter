package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;

/**
 * Date: 6/13/15
 * Time: 1:50 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrenciesRepository {

    ImmutableList<CurrencyUnit> getRegisteredCurrencies();

    void registerCurrency(CurrencyUnit unit);

    ImmutableList<CurrencyUnit> searchCurrenciesByString(String str);

}
