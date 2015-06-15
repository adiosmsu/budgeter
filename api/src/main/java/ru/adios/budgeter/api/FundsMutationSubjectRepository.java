package ru.adios.budgeter.api;

import com.google.common.collect.ImmutableList;

import java.util.Optional;

/**
 * Date: 6/13/15
 * Time: 3:13 AM
 *
 * @author Mikhail Kulikov
 */
public interface FundsMutationSubjectRepository {

    Optional<FundsMutationSubject> findById(int id);

    Optional<FundsMutationSubject> findByName(String name);

    Optional<FundsMutationSubject> findByParent(int parentId);

    ImmutableList<FundsMutationSubject> searchByString(String str);

    void addSubject(FundsMutationSubject subject);

}
