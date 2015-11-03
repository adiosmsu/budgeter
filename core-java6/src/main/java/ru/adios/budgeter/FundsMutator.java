package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import javax.annotation.Nonnull;
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
                BalanceAccount account,
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
            @Nonnull
            @Override
            Money amountToSet(Money amount) {
                return amount.getAmount().signum() >= 0 ? amount : amount.negated();
            }

            @Override
            MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural) {
                return customMoreThanNatural ? BENEFIT : LOSS;
            }

            @Override
            Money getAppropriateMutationAmount(Money amount, Money paidAmount) {
                return amount;
            }
        },
        LOSS {
            @Nonnull
            @Override
            Money amountToSet(Money amount) {
                return amount.getAmount().signum() >= 0 ? amount.negated() : amount;
            }

            @Override
            MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural) {
                return customMoreThanNatural ? LOSS : BENEFIT;
            }

            @Override
            Money getAppropriateMutationAmount(Money amount, Money paidAmount) {
                return paidAmount;
            }
        };

        static MutationDirection forEvent(FundsMutationEvent event) {
            return event.amount.getAmount().signum() >= 0 ? MutationDirection.BENEFIT : MutationDirection.LOSS;
        }

        final BalanceAccount register(Accounter accounter, Treasury treasury, FundsMutationEvent.Builder eventBuilder, Money amount, boolean mutateFunds) {
            final FundsMutationEvent event = eventBuilder.setAmount(amountToSet(amount)).build();
            accounter.fundsMutationEventRepository().register(event);
            if (mutateFunds) {
                treasury.addAmount(event.amount.multipliedBy(event.quantity), event.relevantBalance.name);
                return treasury.getAccountForName(event.relevantBalance.name).orElse(null);
            }
            return null;
        }

        final void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit paidUnit, Optional<BigDecimal> customRate) {
            accounter.postponedFundsMutationEventRepository().rememberPostponedExchangeableEvent(event, paidUnit, customRate);
        }

        @Nonnull
        abstract Money amountToSet(Money amount);

        abstract MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural);

        abstract Money getAppropriateMutationAmount(Money amount, Money paidAmount);

    }

}
