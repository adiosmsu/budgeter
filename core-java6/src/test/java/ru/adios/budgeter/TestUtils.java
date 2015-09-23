package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.temporal.ChronoUnit;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.inmemrepo.Schema;

/**
 * Date: 03.07.15
 * Time: 22:46
 *
 * @author Mikhail Kulikov
 */
public final class TestUtils {

    public static final UtcDay TODAY = new UtcDay();
    public static final UtcDay YESTERDAY = new UtcDay(TODAY.inner.minus(1, ChronoUnit.DAYS));
    public static final UtcDay DAY_BF_YESTER = new UtcDay(YESTERDAY.inner.minus(1, ChronoUnit.DAYS));

    public static final UtcDay JULY_3RD_2015 = new UtcDay(OffsetDateTime.of(2015, 7, 3, 0, 0, 0, 0, ZoneOffset.UTC));

    static Treasury.BalanceAccount prepareBalance(CurrencyUnit unit) {
        final Treasury.BalanceAccount account = new Treasury.BalanceAccount("account" + unit.getCode(), unit);
        Schema.TREASURY.registerBalanceAccount(account);
        return account;
    }

    private TestUtils() {}

}
