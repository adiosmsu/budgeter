package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Date: 6/13/15
 * Time: 7:28 PM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutator {

    Accounter getAccounter();

    Treasury getTreasury();

    CurrenciesExchangeService getRatesService();

    static void registerExchangeDifference(
            FundsMutator mutator,
            Money naturalAmount,
            Money customAmount,
            MutationDirection direction,
            FundsMutationAgent agent,
            OffsetDateTime timestamp,
            int quantity
    ) {
        final int customVsNatural = customAmount.compareTo(naturalAmount);
        if (customVsNatural != 0) {
            final Accounter accounter = mutator.getAccounter();
            final FundsMutationElementCore rec = new FundsMutationElementCore(accounter, mutator.getTreasury(), mutator.getRatesService());
            rec.setQuantity(1);
            rec.setAmount(customAmount.minus(naturalAmount).abs().multipliedBy(quantity));
            rec.setSubject(FundsMutationSubject.getCurrencyConversionDifferenceSubject(accounter.fundsMutationSubjectRepo()));
            rec.setDirection(direction.getExchangeDifferenceDirection(customVsNatural > 0));
            rec.setTimestamp(timestamp);
            rec.setAgent(agent);
            rec.submit();
        }
    }

    enum MutationDirection {

        BENEFIT {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds) {
                final FundsMutationEvent event = eventBuilder.setAmount(amount.getAmount().signum() >= 0 ? amount : amount.negated()).build();
                accounter.registerBenefit(event);
                if (mutateFunds) {
                    treasury.addAmount(event.amount.multipliedBy(event.quantity));
                }
            }

            @Override
            void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit payedUnit, Optional<BigDecimal> customRate) {
                accounter.rememberPostponedExchangeableBenefit(event, payedUnit, customRate);
            }

            @Override
            MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural) {
                return customMoreThanNatural ? BENEFIT : LOSS;
            }
        },
        LOSS {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds) {
                final FundsMutationEvent event = eventBuilder.setAmount(amount.getAmount().signum() >= 0 ? amount.negated() : amount).build();
                accounter.registerLoss(event);
                if (mutateFunds) {
                    treasury.addAmount(event.amount.multipliedBy(event.quantity));
                }
            }

            @Override
            void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit payedUnit, Optional<BigDecimal> customRate) {
                accounter.rememberPostponedExchangeableLoss(event, payedUnit, customRate);
            }

            @Override
            MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural) {
                return customMoreThanNatural ? LOSS : BENEFIT;
            }
        };

        static MutationDirection forEvent(FundsMutationEvent event) {
            return event.amount.getAmount().signum() >= 0 ? MutationDirection.BENEFIT : MutationDirection.LOSS;
        }

        abstract void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds);

        abstract void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit payedUnit, Optional<BigDecimal> customRate);

        abstract MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural);

    }

}
