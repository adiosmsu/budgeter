package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.Treasury;

import java.math.BigDecimal;
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

    static void registerExchangeDifference(FundsMutator mutator, Money naturalAmount, Money customAmount, MutationDirection direction, int quantity) {
        final int customVsNatural = customAmount.compareTo(naturalAmount);
        if (customVsNatural != 0) {
            final Accounter accounter = mutator.getAccounter();
            final FundsMutationElementCore rec = new FundsMutationElementCore(accounter, mutator.getTreasury(), mutator.getRatesService());
            rec.setQuantity(1);
            rec.setAmount(customAmount.minus(naturalAmount).abs().multipliedBy(quantity));
            rec.setSubject(FundsMutationSubject.getCurrencyConversionDifference(accounter.fundsMutationSubjectRepo()));
            rec.setDirection(direction.getExchangeDifferenceDirection(customVsNatural > 0));
            rec.submit();
        }
    }

    enum MutationDirection {

        BENEFIT {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent event) {
                accounter.registerBenefit(event);
                treasury.addAmount(event.amount.multipliedBy(event.quantity));
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
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent event) {
                accounter.registerLoss(event);
                treasury.addAmount(event.amount.multipliedBy(event.quantity).negated());
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

        abstract void register(Accounter accounter, Treasury treasury, FundsMutationEvent event);

        abstract void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit payedUnit, Optional<BigDecimal> customRate);

        abstract MutationDirection getExchangeDifferenceDirection(boolean customMoreThanNatural);

    }

}
