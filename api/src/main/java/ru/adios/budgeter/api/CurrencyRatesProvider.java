package ru.adios.budgeter.api;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;
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

    int RATES_SCALE = 4;

    default Optional<BigDecimal> getConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return getConversionMultiplier(new UtcDay(), from, to);
    }

    Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to);

    Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to);

    default BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        final Optional<BigDecimal> opt = getLatestOptionalConversionMultiplier(from, to);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            final CurrencyUnit rub = CurrencyUnit.of("RUB");
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

    boolean isRateStale(CurrencyUnit to);

    static BigDecimal getConversionMultiplierFromIntermediateMultipliers(BigDecimal interToFirst, BigDecimal interToSecond) {
        return interToFirst.divide(interToSecond, RATES_SCALE, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros();
    }

    static BigDecimal reverseRate(BigDecimal rate) {
        return BigDecimal.ONE.divide(rate, CurrencyRatesProvider.RATES_SCALE, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros();
    }

    static Stream<ConversionPair> streamConversionPairs(Set<CurrencyUnit> unitsSet) {
        final CurrencyUnit[] wrapper = new CurrencyUnit[1];
        return unitsSet.stream()
                .filter(unit -> wrapper[0] != null || (wrapper[0] = unit) == null)
                .map(unit -> new ConversionPair(wrapper[0], unit));
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
