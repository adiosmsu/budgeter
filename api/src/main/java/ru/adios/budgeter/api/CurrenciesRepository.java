package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import org.joda.money.CurrencyUnit;

import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 1:50 AM
 *
 * @author Mikhail Kulikov
 */
public interface CurrenciesRepository {

    Stream<CurrencyUnit> getRegisteredCurrencies();

    void registerCurrency(CurrencyUnit unit);

    ImmutableList<CurrencyUnit> searchCurrenciesByString(String str);

}
