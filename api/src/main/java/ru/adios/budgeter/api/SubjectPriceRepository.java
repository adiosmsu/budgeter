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

import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 11/9/15
 * Time: 10:11 AM
 *
 * @author Mikhail Kulikov
 */
public interface SubjectPriceRepository extends Provider<SubjectPrice, Long>{

    enum Field implements OrderedField, Serializable {
        DAY, PRICE
    }

    void register(SubjectPrice subjectPrice);

    boolean priceExists(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day);

    int countByAgent(FundsMutationSubject subject, FundsMutationAgent agent);

    int countByAgent(String subjectName, String agentName);

    Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    default Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamByAgent(subject, agent, pair.options, pair.limit);
    }

    Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    default Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return streamByAgent(subjectName, agentName, pair.options, pair.limit);
    }

    int count(FundsMutationSubject subject);

    int count(String subjectName);

    Stream<SubjectPrice> stream(FundsMutationSubject subject, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    default Stream<SubjectPrice> stream(FundsMutationSubject subject, RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return stream(subject, pair.options, pair.limit);
    }

    Stream<SubjectPrice> stream(String subjectName, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    default Stream<SubjectPrice> stream(String subjectName, RepoOption... options) {
        final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
        return stream(subjectName, pair.options, pair.limit);
    }

}
