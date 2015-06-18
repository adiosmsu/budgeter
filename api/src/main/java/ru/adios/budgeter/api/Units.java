package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

/**
 * Date: 6/18/15
 * Time: 6:37 PM
 *
 * @author Mikhail Kulikov
 */
public final class Units {

    public static final CurrencyUnit RUB = CurrencyUnit.of("RUB");
    public static final CurrencyUnit BTC = CurrencyUnit.of("BTC");

    private Units() {}

}
