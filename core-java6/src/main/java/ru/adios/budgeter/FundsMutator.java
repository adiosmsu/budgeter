package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 7:28 PM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutator {

    final class Static {

        public static void registerExchangeDifference(
                FundsMutator mutator,
                Money naturalAmount,
                Money customAmount,
                Treasury.BalanceAccount account,
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
                rec.setMutateFunds(false);
                rec.setRelevantBalance(account);
                rec.submit();
            }
        }

    }

    Accounter getAccounter();

    Treasury getTreasury();

    CurrenciesExchangeService getRatesService();

    enum MutationDirection {

        BENEFIT {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds) {
                final FundsMutationEvent event = eventBuilder.setAmount(amount.getAmount().signum() >= 0 ? amount : amount.negated()).build();
                accounter.registerBenefit(event);
                if (mutateFunds) {
                    treasury.addAmount(event.amount.multipliedBy(event.quantity), event.relevantBalance.name);
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

            @Override
            Money getAppropriateMutationAmount(Money amount, Money payedAmount) {
                return amount;
            }
        },
        LOSS {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds) {
                final FundsMutationEvent event = eventBuilder.setAmount(amount.getAmount().signum() >= 0 ? amount.negated() : amount).build();
                accounter.registerLoss(event);
                if (mutateFunds) {
                    treasury.addAmount(event.amount.multipliedBy(event.quantity), event.relevantBalance.name);
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

            @Override
            Money getAppropriateMutationAmount(Money amount, Money payedAmount) {
                return payedAmount;
            }
        };

        static MutationDirection forEvent(FundsMutationEvent event) {
            return event.amount.getAmount().signum() >= 0 ? MutationDirection.BENEFIT : MutationDirection.LOSS;
        }

        abstract void register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds);

        abstract void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit payedUnit, Optional<BigDecimal> customRate);

        abstract MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural);

        abstract Money getAppropriateMutationAmount(Money amount, Money payedAmount);

    }

}
