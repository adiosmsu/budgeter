package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 3:13 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationSubjectRepository {

    final class Default {

        private final FundsMutationSubjectRepository fundsMutationSubjectRepository;

        public Default(FundsMutationSubjectRepository fundsMutationSubjectRepository) {
            this.fundsMutationSubjectRepository = fundsMutationSubjectRepository;
        }

        public FundsMutationSubject addSubject(FundsMutationSubject subject) {
            if (subject.parentId == 0) {
                final FundsMutationSubject.Builder builder = FundsMutationSubject.builder(fundsMutationSubjectRepository).setFundsMutationSubject(subject);
                if (!subject.id.isPresent())
                    builder.setId(fundsMutationSubjectRepository.idSeqNext());
                subject = builder.build();
            }
            fundsMutationSubjectRepository.rawAddition(subject);
            if (subject.parentId != 0) {
                fundsMutationSubjectRepository.updateChildFlag(subject.parentId);
            }
            return subject;
        }

    }

    Optional<FundsMutationSubject> findById(int id);

    Optional<FundsMutationSubject> findByName(String name);

    Stream<FundsMutationSubject> findByParent(int parentId);

    ImmutableList<FundsMutationSubject> nameLikeSearch(String str);

    FundsMutationSubject addSubject(FundsMutationSubject subject); // default in java8

    void rawAddition(FundsMutationSubject subject);

    int idSeqNext();

    int getIdForRateSubject();

    void updateChildFlag(int id);

}
