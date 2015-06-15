package ru.adios.budgeter;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import ru.adios.budgeter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    static void registerExchangeDifference(FundsMutator mutator, Money naturalAmount, Money customAmount, MutationDirection directionIfCustomMore, int quantity) {
        final int customVsNatural = customAmount.compareTo(naturalAmount);
        if (customVsNatural != 0) {
            final Accounter accounter = mutator.getAccounter();
            final FundsMutationElementCore rec = new FundsMutationElementCore(accounter, mutator.getTreasury(), mutator.getRatesService());
            rec.setQuantity(1);
            rec.setAmount(customAmount.minus(naturalAmount).abs().multipliedBy(quantity));
            rec.setSubject(FundsMutationSubject.getCurrencyConversionDifference(accounter.fundsMutationSubjectRepo()));
            rec.setDirection(customVsNatural > 0 ? directionIfCustomMore : directionIfCustomMore.other());
            rec.register();
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
            void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit convUnit, Optional<BigDecimal> customRate) {
                accounter.rememberPostponedExchangeableBenefit(event, convUnit, customRate);
            }

            @Override
            Money getActualAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate) {
                return amount;
            }

            @Override
            Money getOtherPartyAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate) {
                return amount.convertedTo(convUnit, rate, RoundingMode.HALF_DOWN);
            }

            @Override
            Money getConvertedAmount(Money actualAmount, Money otherPartyAmount) {
                return otherPartyAmount;
            }

            @Override
            MutationDirection other() {
                return LOSS;
            }
        },
        LOSS {
            @Override
            void register(Accounter accounter, Treasury treasury, FundsMutationEvent event) {
                accounter.registerLoss(event);
                treasury.addAmount(event.amount.multipliedBy(event.quantity).negated());
            }

            @Override
            void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit convUnit, Optional<BigDecimal> customRate) {
                accounter.rememberPostponedExchangeableLoss(event, convUnit, customRate);
            }

            @Override
            Money getActualAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate) {
                return amount.convertedTo(convUnit, rate, RoundingMode.HALF_DOWN);
            }

            @Override
            Money getOtherPartyAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate) {
                return amount;
            }

            @Override
            Money getConvertedAmount(Money actualAmount, Money otherPartyAmount) {
                return actualAmount;
            }

            @Override
            MutationDirection other() {
                return BENEFIT;
            }
        };

        abstract void register(Accounter accounter, Treasury treasury, FundsMutationEvent event);

        abstract void remember(Accounter accounter, FundsMutationEvent event, CurrencyUnit convUnit, Optional<BigDecimal> customRate);

        abstract Money getActualAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate);

        abstract Money getOtherPartyAmount(Money amount, CurrencyUnit convUnit, BigDecimal rate);

        abstract Money getConvertedAmount(Money actualAmount, Money otherPartyAmount);

        abstract MutationDirection other();

    }

}
