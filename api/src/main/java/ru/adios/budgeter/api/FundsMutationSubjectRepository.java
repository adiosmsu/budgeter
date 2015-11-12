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

package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 3:13 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationSubjectRepository extends Provider<FundsMutationSubject, Long> {

    int RATES_ID = 1;

    Optional<FundsMutationSubject> findByName(String name);

    Stream<FundsMutationSubject> findByParent(long parentId);

    Stream<FundsMutationSubject> streamAll();

    ImmutableList<FundsMutationSubject> nameLikeSearch(String str);

    default FundsMutationSubject addSubject(FundsMutationSubject subject) {
        subject = rawAddition(subject);
        if (subject.parentId != 0) {
            updateChildFlag(subject.parentId);
        }
        return subject;
    }

    FundsMutationSubject rawAddition(FundsMutationSubject subject);

    default long getIdForRateSubject() {
        return RATES_ID;
    }

    void updateChildFlag(long id);

}
