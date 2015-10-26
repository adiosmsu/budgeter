package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Date: 6/13/15
 * Time: 3:13 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationSubjectRepository extends Provider<FundsMutationSubject, Long> {

    Optional<FundsMutationSubject> findByName(String name);

    Stream<FundsMutationSubject> findByParent(long parentId);

    Stream<FundsMutationSubject> streamAll();

    ImmutableList<FundsMutationSubject> nameLikeSearch(String str);

    default FundsMutationSubject addSubject(FundsMutationSubject subject) {
        if (subject.parentId == 0) {
            final FundsMutationSubject.Builder builder = FundsMutationSubject.builder(this).setFundsMutationSubject(subject);
            if (!subject.id.isPresent())
                builder.setId(idSeqNext());
            subject = builder.build();
        }
        rawAddition(subject);
        if (subject.parentId != 0) {
            updateChildFlag(subject.parentId);
        }
        return subject;
    }

    void rawAddition(FundsMutationSubject subject);

    long idSeqNext();

    long getIdForRateSubject();

    void updateChildFlag(long id);

}
