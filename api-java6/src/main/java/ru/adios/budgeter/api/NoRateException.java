package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 9/25/15
 * Time: 4:16 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public final class NoRateException extends BudgeterApiException {

    public final CurrencyUnit first;
    public final CurrencyUnit second;

    public NoRateException(CurrencyUnit first, CurrencyUnit second) {
        super("Unknown exchange rate between " + first + " and " + second);
        this.first = first;
        this.second = second;
    }

}
