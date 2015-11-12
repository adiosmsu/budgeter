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

import java8.util.Optional;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import java.io.Serializable;
import java.util.List;

/**
 * Date: 11/9/15
 * Time: 10:11 AM
 *
 * @author Mikhail Kulikov
 */
public interface SubjectPriceRepository extends Provider<SubjectPrice, Long>{

    enum Field implements OrderedField, Serializable
    {
        DAY, PRICE
    }

    final class Default {

        private SubjectPriceRepository repo;

        public Default(SubjectPriceRepository repo) {
            this.repo = repo;
        }

        public Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.streamByAgent(subjectId, agentId, pair.options, pair.limit);
        }

        public Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.streamByAgent(subject, agent, pair.options, pair.limit);
        }

        public Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.streamByAgent(subjectName, agentName, pair.options, pair.limit);
        }

        public Stream<SubjectPrice> stream(long subjectId, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.stream(subjectId, pair.options, pair.limit);
        }

        public Stream<SubjectPrice> stream(FundsMutationSubject subject, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.stream(subject, pair.options, pair.limit);
        }

        public Stream<SubjectPrice> stream(String subjectName, RepoOption... options) {
            final RepoUtil.Pair<Field> pair = RepoUtil.parseOptVarArg(options, Field.class);
            return repo.stream(subjectName, pair.options, pair.limit);
        }

    }

    void register(SubjectPrice subjectPrice);

    boolean priceExists(FundsMutationSubject subject, FundsMutationAgent agent, UtcDay day);

    int countByAgent(FundsMutationSubject subject, FundsMutationAgent agent);

    int countByAgent(String subjectName, String agentName);

    Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> streamByAgent(long subjectId, long agentId, RepoOption... options);

    Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> streamByAgent(FundsMutationSubject subject, FundsMutationAgent agent, RepoOption... options); // default in java8

    Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> streamByAgent(String subjectName, String agentName, RepoOption... options);  // default in java8

    int count(FundsMutationSubject subject);

    int count(String subjectName);

    Stream<SubjectPrice> stream(long subjectId, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> stream(long subjectId, RepoOption... options);

    Stream<SubjectPrice> stream(FundsMutationSubject subject, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> stream(FundsMutationSubject subject, RepoOption... options); // default in java8

    Stream<SubjectPrice> stream(String subjectName, List<OrderBy<Field>> options, Optional<OptLimit> limit);

    Stream<SubjectPrice> stream(String subjectName, RepoOption... options); // default in java8

}
