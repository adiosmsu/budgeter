package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.math.IntMath;
import org.joda.money.CurrencyUnit;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/12/15
 * Time: 7:03 PM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyRatesProvider {

    int RATES_SCALE = 24;

    default Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        final Optional<BigDecimal> straight = getConversionMultiplierBidirectional(day, from, to);
        if (straight.isPresent()) {
            return straight;
        } else if (!Units.RUB.equals(from) && !Units.RUB.equals(to)) {
            final CurrencyUnit rub = Units.RUB;
            return getConversionMultiplierWithIntermediate(day, from, to, rub);
        } else {
            return Optional.empty();
        }
    }

    default Optional<BigDecimal> getConversionMultiplierBidirectional(final UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplierInBidirectionalWay(
                (unit, unit2) -> getConversionMultiplierStraight(day, unit, unit2),
                from,
                to
        );
    }

    default Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        final Optional<BigDecimal> rubToFirst = getConversionMultiplierBidirectional(day, intermediate, from);
        if (!rubToFirst.isPresent())
            return rubToFirst;
        final Optional<BigDecimal> rubToSecond = getConversionMultiplierBidirectional(day, intermediate, to);
        if (!rubToSecond.isPresent())
            return rubToSecond;
        return Optional.of(getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get()));
    }

    Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to);


    default Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplierInBidirectionalWay(this::getLatestOptionalConversionMultiplier, from, to);
    }

    Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to);

    @Nullable
    default BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        final Optional<BigDecimal> opt = getLatestOptionalConversionMultiplierBidirectional(from, to);
        if (opt.isPresent()) {
            return opt.get();
        } else if (!Units.RUB.equals(from) && !Units.RUB.equals(to)) {
            final CurrencyUnit rub = Units.RUB;
            return getLatestConversionMultiplierWithIntermediate(from, to, rub);
        } else {
            return null;
        }
    }

    default BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
        final Optional<BigDecimal> rubToFirst = getLatestOptionalConversionMultiplierBidirectional(intermediate, from);
        checkState(rubToFirst.isPresent(), "No rate from %s to %s", intermediate, from);
        final Optional<BigDecimal> rubToSecond = getLatestOptionalConversionMultiplierBidirectional(intermediate, to);
        checkState(rubToSecond.isPresent(), "No rate from %s to %s", intermediate, to);
        return getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get());
    }

    boolean isRateStale(CurrencyUnit to);


    static Optional<BigDecimal> getConversionMultiplierInBidirectionalWay(BiFunction<CurrencyUnit, CurrencyUnit, Optional<BigDecimal>> straightGetter, CurrencyUnit from, CurrencyUnit to) {
        Optional<BigDecimal> result = straightGetter.apply(from, to);

        if (result.isPresent()) {
            return result;
        }

        result = straightGetter.apply(to, from);

        if (result.isPresent()) {
            return Optional.of(reverseRate(result.get()));
        }

        return Optional.empty();
    }

    static BigDecimal getConversionMultiplierFromIntermediateMultipliers(BigDecimal interToFirst, BigDecimal interToSecond) {
        return interToSecond.divide(interToFirst, RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
    }

    static BigDecimal reverseRate(BigDecimal rate) {
        return BigDecimal.ONE.divide(rate, CurrencyRatesProvider.RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
    }

    /**
     * Orientation is: [buy amount] = [sell amount] * rate.
     * @param bought amount bought
     * @param sold amount sold
     * @return conversion rate
     */
    static BigDecimal calculateRate(BigDecimal bought, BigDecimal sold) {
        return bought.divide(sold, RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
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
