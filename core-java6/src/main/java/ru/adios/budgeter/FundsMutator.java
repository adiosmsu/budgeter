/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter;

import java8.util.Optional;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.threeten.bp.OffsetDateTime;
import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.SubjectPriceRepository;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.*;

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
                final SubjectPriceRepository repository = accounter.subjectPriceRepository();
                final UtcDay day = new UtcDay(event.timestamp);
                if (!repository.priceExists(event.subject, event.agent, day)) {
                    repository.register(
                            SubjectPrice.builder()
                                    .setDay(day)
                                    .setPrice(amount.abs())
                                    .setSubject(event.subject)
                                    .setAgent(event.agent)
                                    .build()
                    );
                }
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
