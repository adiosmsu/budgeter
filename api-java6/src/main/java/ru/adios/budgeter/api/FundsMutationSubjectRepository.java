package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.data.FundsMutationSubject;

/**
 * Date: 6/13/15
 * Time: 3:13 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationSubjectRepository extends Provider<FundsMutationSubject, Long> {

    int RATES_ID = 1;

    final class Default {

        private final FundsMutationSubjectRepository fundsMutationSubjectRepository;

        public Default(FundsMutationSubjectRepository fundsMutationSubjectRepository) {
            this.fundsMutationSubjectRepository = fundsMutationSubjectRepository;
        }

        public FundsMutationSubject addSubject(FundsMutationSubject subject) {
            subject = fundsMutationSubjectRepository.rawAddition(subject);
            if (subject.parentId != 0) {
                fundsMutationSubjectRepository.updateChildFlag(subject.parentId);
            }
            return subject;
        }

        public long getIdForRateSubject() {
            return RATES_ID;
        }

    }

    Optional<FundsMutationSubject> findByName(String name);

    Stream<FundsMutationSubject> findByParent(long parentId);

    Stream<FundsMutationSubject> streamAll();

    ImmutableList<FundsMutationSubject> nameLikeSearch(String str);

    FundsMutationSubject addSubject(FundsMutationSubject subject); // default in java8

    FundsMutationSubject rawAddition(FundsMutationSubject subject);

    long getIdForRateSubject(); // default in java8

    void updateChildFlag(long id);

}
