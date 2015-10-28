package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.IntMath;
import java8.util.Optional;
import java8.util.function.BiFunction;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import org.joda.money.CurrencyUnit;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Date: 6/12/15
 * Time: 7:03 PM
 *
 * @author Mikhail Kulikov
 */
public interface CurrencyRatesProvider extends Provider<CurrencyRatesProvider.ConversionRate, Long> {

    int RATES_SCALE = 24;

    final class Static {

        public static Optional<BigDecimal> getConversionMultiplierInBidirectionalWay(BiFunction<CurrencyUnit, CurrencyUnit, Optional<BigDecimal>> straightGetter, CurrencyUnit from, CurrencyUnit to) {
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

        public static BigDecimal getConversionMultiplierFromIntermediateMultipliers(BigDecimal interToFirst, BigDecimal interToSecond) {
            return interToSecond.divide(interToFirst, RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
        }

        public static BigDecimal reverseRate(BigDecimal rate) {
            return BigDecimal.ONE.divide(rate, CurrencyRatesProvider.RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
        }

        /**
         * Orientation is: [buy amount] = [sell amount] * rate.
         * @param bought amount bought
         * @param sold amount sold
         * @return conversion rate
         */
        public static BigDecimal calculateRate(BigDecimal bought, BigDecimal sold) {
            return bought.divide(sold, RATES_SCALE, RoundingMode.HALF_DOWN).stripTrailingZeros();
        }

        public static Stream<ConversionPair> streamConversionPairs(Set<CurrencyUnit> unitsSet) {
            final int sizeUnits = unitsSet.size();
            if (sizeUnits <= 1)
                return StreamSupport.empty();
            if (sizeUnits == 2) {
                final Iterator<CurrencyUnit> it = unitsSet.iterator();
                return StreamSupport.of(new ConversionPair(it.next(), it.next()));
            }
            final ImmutableList<CurrencyUnit> currencyUnitsList = ImmutableList.copyOf(unitsSet);

            final ArrayList<ConversionPair> builder = new ArrayList<ConversionPair>(IntMath.binomial(sizeUnits, 2) + 1);
            for (int j = 0; j < currencyUnitsList.size() - 1; j++) {
                final CurrencyUnit first = currencyUnitsList.get(j);
                for (int i = j + 1; i < currencyUnitsList.size(); i++) {
                    builder.add(new ConversionPair(first, currencyUnitsList.get(i)));
                }
            }
            return StreamSupport.stream(builder);
        }

    }

    final class Default {

        private final CurrencyRatesProvider currencyRatesProvider;

        public Default(CurrencyRatesProvider currencyRatesProvider) {
            this.currencyRatesProvider = currencyRatesProvider;
        }

        public Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
            final Optional<BigDecimal> straight = currencyRatesProvider.getConversionMultiplierBidirectional(day, from, to);
            if (straight.isPresent()) {
                return straight;
            } else if (!Units.RUB.equals(from) && !Units.RUB.equals(to)) {
                final CurrencyUnit rub = Units.RUB;
                return currencyRatesProvider.getConversionMultiplierWithIntermediate(day, from, to, rub);
            } else {
                return Optional.empty();
            }
        }

        public Optional<BigDecimal> getConversionMultiplierBidirectional(final UtcDay day, CurrencyUnit from, CurrencyUnit to) {
            return Static.getConversionMultiplierInBidirectionalWay(
                    new BiFunction<CurrencyUnit, CurrencyUnit, Optional<BigDecimal>>() {
                        @Override
                        public Optional<BigDecimal> apply(CurrencyUnit unit, CurrencyUnit unit2) {
                            return currencyRatesProvider.getConversionMultiplierStraight(day, unit, unit2);
                        }
                    },
                    from,
                    to
            );
        }

        public Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
            final Optional<BigDecimal> rubToFirst = getConversionMultiplierBidirectional(day, intermediate, from);
            if (!rubToFirst.isPresent())
                return rubToFirst;
            final Optional<BigDecimal> rubToSecond = getConversionMultiplierBidirectional(day, intermediate, to);
            if (!rubToSecond.isPresent())
                return rubToSecond;
            return Optional.of(Static.getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get()));
        }

        public Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to) {
            return Static.getConversionMultiplierInBidirectionalWay(new BiFunction<CurrencyUnit, CurrencyUnit, Optional<BigDecimal>>() {
                @Override
                public Optional<BigDecimal> apply(CurrencyUnit unit, CurrencyUnit unit2) {
                    return currencyRatesProvider.getLatestOptionalConversionMultiplier(unit, unit2);
                }
            }, from, to);
        }

        @Nullable
        public BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
            final Optional<BigDecimal> opt = getLatestOptionalConversionMultiplierBidirectional(from, to);
            if (opt.isPresent()) {
                return opt.get();
            } else if (!Units.RUB.equals(from) && !Units.RUB.equals(to)) {
                final CurrencyUnit rub = Units.RUB;
                return currencyRatesProvider.getLatestConversionMultiplierWithIntermediate(from, to, rub);
            } else {
                return null;
            }
        }

        public BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate) {
            final Optional<BigDecimal> rubToFirst = getLatestOptionalConversionMultiplierBidirectional(intermediate, from);
            checkState(rubToFirst.isPresent(), "No rate from %s to %s", intermediate, from);
            final Optional<BigDecimal> rubToSecond = getLatestOptionalConversionMultiplierBidirectional(intermediate, to);
            checkState(rubToSecond.isPresent(), "No rate from %s to %s", intermediate, to);
            return Static.getConversionMultiplierFromIntermediateMultipliers(rubToFirst.get(), rubToSecond.get());
        }

    }

    Optional<BigDecimal> getConversionMultiplier(UtcDay day, CurrencyUnit from, CurrencyUnit to); // default in java8

    Optional<BigDecimal> getConversionMultiplierBidirectional(final UtcDay day, CurrencyUnit from, CurrencyUnit to); // default in java8

    Optional<BigDecimal> getConversionMultiplierWithIntermediate(UtcDay day, CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate); // default in java8

    Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to);


    Optional<BigDecimal> getLatestOptionalConversionMultiplierBidirectional(CurrencyUnit from, CurrencyUnit to); // default in java8

    Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to);

    @Nullable
    BigDecimal getLatestConversionMultiplier(CurrencyUnit from, CurrencyUnit to); // default in java8

    BigDecimal getLatestConversionMultiplierWithIntermediate(CurrencyUnit from, CurrencyUnit to, CurrencyUnit intermediate); // default in java8

    boolean isRateStale(CurrencyUnit to);

    ImmutableSet<Long> getIndexedForDay(UtcDay day);

    final class ConversionPair {

        public final CurrencyUnit from;
        public final CurrencyUnit to;

        public ConversionPair(CurrencyUnit from, CurrencyUnit to) {
            checkNotNull(from, "from");
            checkNotNull(to, "to");

            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConversionPair that = (ConversionPair) o;

            return from.equals(that.from)
                    && to.equals(that.to);
        }

        @Override
        public int hashCode() {
            int result = from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

    }

    final class ConversionRate {

        public final UtcDay day;
        public final ConversionPair pair;
        public final BigDecimal rate;

        public ConversionRate(UtcDay day, ConversionPair pair, BigDecimal rate) {
            checkNotNull(day, "day");
            checkNotNull(pair, "pair");
            checkNotNull(rate, "rate");
            this.day = day;
            this.pair = pair;
            this.rate = rate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConversionRate that = (ConversionRate) o;

            return day.equals(that.day)
                    && pair.equals(that.pair)
                    && rate.equals(that.rate);
        }

        @Override
        public int hashCode() {
            int result = day.hashCode();
            result = 31 * result + pair.hashCode();
            result = 31 * result + rate.hashCode();
            return result;
        }

    }

}
