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
