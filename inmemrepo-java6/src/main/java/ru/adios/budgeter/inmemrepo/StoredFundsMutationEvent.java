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

package ru.adios.budgeter.inmemrepo;

import ru.adios.budgeter.api.data.FundsMutationEvent;

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
