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
import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.api.data.FundsMutationSubject;

import static org.junit.Assert.*;

/**
 * Date: 10/26/15
 * Time: 4:52 PM
 *
 * @author Mikhail Kulikov
 */
public final class FundsMutationSubjectRepoTester {

    private final Bundle bundle;

    public FundsMutationSubjectRepoTester(Bundle bundle) {
        this.bundle = bundle;
    }

    public void testRawAddition() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject parts = FundsMutationSubject.builder(subjectRepository).setName("Parts").setType(FundsMutationSubject.Type.PRODUCT).build();
        subjectRepository.rawAddition(parts);
        final Long partsId = subjectRepository.currentSeqValue();
        assertEquals("Parts not found after insert", parts, subjectRepository.getById(partsId).get());
        try {
            parts = FundsMutationSubject.builder(subjectRepository).setName("Parts").setType(FundsMutationSubject.Type.OCCASION).build();
            subjectRepository.rawAddition(parts);
            fail("Name unique index failed on \"Parts\"");
        } catch (Exception ignored) {}
        try {
            parts = FundsMutationSubject.builder(subjectRepository).setName("Parts2").setType(FundsMutationSubject.Type.OCCASION).setId(partsId).build();
            subjectRepository.rawAddition(parts);
            fail("Insert on used id passed");
        } catch (Exception ignored) {}
    }

    public void testUpdateChildFlag() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject con = FundsMutationSubject.builder(subjectRepository).setName("Consulting").setType(FundsMutationSubject.Type.SERVICE).build();
        subjectRepository.addSubject(con);
        final Long id = subjectRepository.currentSeqValue();
        subjectRepository.updateChildFlag(id);
        assertTrue("Child flag not updated", subjectRepository.getById(id).get().childFlag);
    }

    public void testFindById() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject con = FundsMutationSubject.builder(subjectRepository).setName("Grooming").setType(FundsMutationSubject.Type.SERVICE).setId(100).build();
        subjectRepository.addSubject(con);
        assertEquals("Grooming not found", con, subjectRepository.getById(100L).get());
    }

    public void testFindByName() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject med = FundsMutationSubject.builder(subjectRepository).setName("Medical").setType(FundsMutationSubject.Type.SERVICE).build();
        subjectRepository.addSubject(med);
        assertEquals("Grooming not found", med, subjectRepository.findByName("Medical").get());
        assertFalse("Non existent found", subjectRepository.findByName("TEstDTdgS").isPresent());
    }

    public void testFindByParent() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        try {
            FundsMutationSubject food = FundsMutationSubject.builder(subjectRepository).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            subjectRepository.addSubject(food);
        } catch (Exception ignore) {}
        final FundsMutationSubject food1 = subjectRepository.findByName("Food").get();
        final FundsMutationSubject meat =
                FundsMutationSubject.builder(subjectRepository).setName("Meat").setType(FundsMutationSubject.Type.PRODUCT).setParentId(food1.id.getAsLong()).build();
        final FundsMutationSubject vegs =
                FundsMutationSubject.builder(subjectRepository).setName("Vegs").setType(FundsMutationSubject.Type.PRODUCT).setParentId(food1.id.getAsLong()).build();
        subjectRepository.addSubject(meat);
        subjectRepository.addSubject(vegs);
        subjectRepository.findByParent(food1.id.getAsLong()).forEach(new Consumer<FundsMutationSubject>() {
            @Override
            public void accept(FundsMutationSubject subject) {
                assertTrue("Wrong byParent stream: " + subject.name, subject.equals(meat) || subject.equals(vegs));
            }
        });
    }

    public void testSearchByString() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject clothes = FundsMutationSubject.builder(subjectRepository).setName("Clothes").setType(FundsMutationSubject.Type.PRODUCT).build();
        subjectRepository.addSubject(clothes);
        FundsMutationSubject cleaners = FundsMutationSubject.builder(subjectRepository).setName("Cleaners").setType(FundsMutationSubject.Type.PRODUCT).build();
        subjectRepository.addSubject(cleaners);
        ImmutableList<FundsMutationSubject> found = subjectRepository.nameLikeSearch("Cl%");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners) || found.get(0).equals(clothes));
        assertTrue("Search strange result " + found.get(1), found.get(1).equals(cleaners) || found.get(1).equals(clothes));
        found = subjectRepository.nameLikeSearch("%s");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners) || found.get(0).equals(clothes));
        assertTrue("Search strange result " + found.get(1), found.get(1).equals(cleaners) || found.get(1).equals(clothes));
        found = subjectRepository.nameLikeSearch("%ers%");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners));
    }

    public void testAddSubject() throws Exception {
        final FundsMutationSubjectRepository subjectRepository = bundle.fundsMutationSubjects();

        FundsMutationSubject cars = FundsMutationSubject.builder(subjectRepository).setName("Cars").setType(FundsMutationSubject.Type.PRODUCT).build();
        subjectRepository.addSubject(cars);
        final Optional<FundsMutationSubject> cars1 = subjectRepository.findByName("Cars");
        final int carsId = (int) cars1.get().id.getAsLong();
        assertTrue("Parent and root did not initialize", cars1.get().parentId == 0 && cars1.get().rootId == 0);
        FundsMutationSubject mitsubishi =
                FundsMutationSubject.builder(subjectRepository).setName("Mitsubishi").setType(FundsMutationSubject.Type.PRODUCT).setParentId(carsId).build();
        subjectRepository.addSubject(mitsubishi);
        final Optional<FundsMutationSubject> mitsubishi1 = subjectRepository.findByName("Mitsubishi");
        assertTrue("Root id of child did not initialize", mitsubishi1.get().rootId == carsId);
        final Optional<FundsMutationSubject> cars2 = subjectRepository.findByName("Cars");
        assertTrue("Child flag did not update", cars2.get().childFlag);
    }

}
