package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.math.IntMath;
import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/12/15
 * Time: 7:03 PM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyRatesProvider {

    int RATES_SCALE = 12;

    default Optional<BigDecimal> getConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(new UtcDay(), from, to);
    }

    default Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        final Optional<BigDecimal> straight = getConversionMultiplierStraight(day, from, to);
        if (straight.isPresent()) {
            return straight;
        } else {
            final CurrencyUnit rub = Units.RUB;
            return getConversionMultiplierWithIntermediate(day, from, to, rub);
        }
    }

    Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to);

    Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to);

    default BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        final Optional<BigDecimal> opt = getLatestOptionalConversionMultiplier(from, to);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            final CurrencyUnit rub = Units.RUB;
            return getLatestConversionMultiplierWithIntermediate(from, to, rub);
        }
    }

    default BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        final Optional<BigDecimal> rubToFirst = getLatestOptionalConversionMultiplier(intermediate, from);
        checkState(rubToFirst.isPresent(), "No rate from %s to %s", intermediate, from);
        final Optional<BigDecimal> rubToSecond = getLatestOptionalConversionMultiplier(intermediate, to);
        checkState(rubToSecond.isPresent(), "No rate from %s to %s", intermediate, to);
        return getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get());
    }

    default Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        final Optional<BigDecimal> rubToFirst = getConversionMultiplierStraight(day, intermediate, from);
        if (!rubToFirst.isPresent())
            return rubToFirst;
        final Optional<BigDecimal> rubToSecond = getConversionMultiplierStraight(day, intermediate, to);
        if (!rubToSecond.isPresent())
            return rubToSecond;
        return Optional.of(getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get()));
    }

    boolean isRateStale(CurrencyUnit to);

    static BigDecimal getConversionMultiplierFromIntermediateMultipliers(BigDecimal interToFirst, BigDecimal interToSecond) {
        return interToFirst.divide(interToSecond, RATES_SCALE, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros();
    }

    static BigDecimal reverseRate(BigDecimal rate) {
        return BigDecimal.ONE.divide(rate, CurrencyRatesProvider.RATES_SCALE, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros();
    }

    static Stream<ConversionPair> streamConversionPairs(Set<CurrencyUnit> unitsSet) {
        final int sizeUnits = unitsSet.size();
        if (sizeUnits <= 1)
            return Stream.empty();
        if (sizeUnits == 2) {
            final Iterator<CurrencyUnit> it = unitsSet.iterator();
            return Stream.of(new ConversionPair(it.next(), it.next()));
        }
        final ImmutableList<CurrencyUnit> currencyUnitsList = ImmutableList.copyOf(unitsSet);

        final ArrayList<ConversionPair> builder = new ArrayList<>(IntMath.binomial(sizeUnits, 2) + 1);
        for (int j = 0; j < currencyUnitsList.size() - 1; j++) {
            final CurrencyUnit first = currencyUnitsList.get(j);
            for (int i = j + 1; i < currencyUnitsList.size(); i++) {
                builder.add(new ConversionPair(first, currencyUnitsList.get(i)));
            }
        }
        return builder.stream();
    }

    final class ConversionPair {

        public final CurrencyUnit from;
        public final CurrencyUnit to;

        public ConversionPair(CurrencyUnit from, CurrencyUnit to) {
            this.from = from;
            this.to = to;
        }

    }

}
