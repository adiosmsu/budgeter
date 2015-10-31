package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.FundsMutationEvent;

/**
 * Date: 6/15/15
 * Time: 9:38 AM
 *
 * @author Mikhail Kulikov
 */
final class StoredFundsMutationEvent extends Stored<FundsMutationEvent> {

    final FundsMutationDirection direction;

    StoredFundsMutationEvent(int id, FundsMutationEvent obj, FundsMutationDirection direction) {
        super(id, obj);
        this.direction = direction;
    }

    FundsMutationEvent constructValid() {
        return FundsMutationEvent.builder()
                .setFundsMutationEvent(obj)
                .setRelevantBalance(Schema.TREASURY.getAccountForName(obj.relevantBalance.name).get())
                .build();
    }

}
