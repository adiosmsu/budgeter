package ru.adios.budgeter.inmemrepo;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationSubject;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Date: 6/15/15
 * Time: 5:28 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectPseudoTableTest {

    @Test
    public void testRawAddition() throws Exception {
        FundsMutationSubject parts = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Parts").setType(FundsMutationSubject.Type.PRODUCT).build();
        Schema.FUNDS_MUTATION_SUBJECTS.rawAddition(parts);
        final int partsId = Schema.FUNDS_MUTATION_SUBJECTS.idSequence.get();
        assertEquals("Parts not found after insert", parts, Schema.FUNDS_MUTATION_SUBJECTS.get(partsId));
        try {
            parts = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Parts").setType(FundsMutationSubject.Type.OCCASION).build();
            Schema.FUNDS_MUTATION_SUBJECTS.rawAddition(parts);
            fail("Name unique index failed on \"Parts\"");
        } catch (Exception ignored) {}
        try {
            parts = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Parts2").setType(FundsMutationSubject.Type.OCCASION).setId(partsId).build();
            Schema.FUNDS_MUTATION_SUBJECTS.rawAddition(parts);
            fail("Insert on used id passed");
        } catch (Exception ignored) {}
    }

    @Test
    public void testUpdateChildFlag() throws Exception {
        FundsMutationSubject con = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Consulting").setType(FundsMutationSubject.Type.SERVICE).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(con);
        final int id = Schema.FUNDS_MUTATION_SUBJECTS.idSequence.get();
        Schema.FUNDS_MUTATION_SUBJECTS.updateChildFlag(id);
        assertTrue("Child flag not updated", Schema.FUNDS_MUTATION_SUBJECTS.get(id).childFlag);
    }

    @Test
    public void testFindById() throws Exception {
        FundsMutationSubject con = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Grooming").setType(FundsMutationSubject.Type.SERVICE).setId(100).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(con);
        assertEquals("Grooming not found", con, Schema.FUNDS_MUTATION_SUBJECTS.findById(100).get());
    }

    @Test
    public void testFindByName() throws Exception {
        FundsMutationSubject med = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Medical").setType(FundsMutationSubject.Type.SERVICE).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(med);
        assertEquals("Grooming not found", med, Schema.FUNDS_MUTATION_SUBJECTS.findByName("Medical").get());
        assertFalse("Non existent found", Schema.FUNDS_MUTATION_SUBJECTS.findByName("TEstDTdgS").isPresent());
    }

    @Test
    public void testFindByParent() throws Exception {
        try {
            FundsMutationSubject food = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Food").setType(FundsMutationSubject.Type.PRODUCT).build();
            Schema.FUNDS_MUTATION_SUBJECTS.addSubject(food);
        } catch (Exception ignore) {}
        final FundsMutationSubject food1 = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Food").get();
        final FundsMutationSubject meat =
                FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Meat").setType(FundsMutationSubject.Type.PRODUCT).setParentId(food1.id.getAsInt()).build();
        final FundsMutationSubject vegs =
                FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Vegs").setType(FundsMutationSubject.Type.PRODUCT).setParentId(food1.id.getAsInt()).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(meat);
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(vegs);
        Schema.FUNDS_MUTATION_SUBJECTS.findByParent(food1.id.getAsInt()).forEach(subject -> assertTrue("Wrong byParent stream: " + subject.name, subject.equals(meat) || subject.equals(vegs)));
    }

    @Test
    public void testSearchByString() throws Exception {
        FundsMutationSubject clothes = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Clothes").setType(FundsMutationSubject.Type.PRODUCT).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(clothes);
        FundsMutationSubject cleaners = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Cleaners").setType(FundsMutationSubject.Type.PRODUCT).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(cleaners);
        ImmutableList<FundsMutationSubject> found = Schema.FUNDS_MUTATION_SUBJECTS.nameLikeSearch("Cl%");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners) || found.get(0).equals(clothes));
        assertTrue("Search strange result " + found.get(1), found.get(1).equals(cleaners) || found.get(1).equals(clothes));
        found = Schema.FUNDS_MUTATION_SUBJECTS.nameLikeSearch("%s");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners) || found.get(0).equals(clothes));
        assertTrue("Search strange result " + found.get(1), found.get(1).equals(cleaners) || found.get(1).equals(clothes));
        found = Schema.FUNDS_MUTATION_SUBJECTS.nameLikeSearch("%ers%");
        assertTrue("Search strange result " + found.get(0), found.get(0).equals(cleaners));
    }

    @Test
    public void testAddSubject() throws Exception {
        FundsMutationSubject cars = FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Cars").setType(FundsMutationSubject.Type.PRODUCT).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(cars);
        final Optional<FundsMutationSubject> cars1 = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Cars");
        final int carsId = cars1.get().id.getAsInt();
        assertTrue("Parent and root did not initialize", cars1.get().parentId == 0 && cars1.get().rootId == 0);
        FundsMutationSubject mitsubishi =
                FundsMutationSubject.builder(Schema.FUNDS_MUTATION_SUBJECTS).setName("Mitsubishi").setType(FundsMutationSubject.Type.PRODUCT).setParentId(carsId).build();
        Schema.FUNDS_MUTATION_SUBJECTS.addSubject(mitsubishi);
        final Optional<FundsMutationSubject> mitsubishi1 = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Mitsubishi");
        assertTrue("Root id of child did not initialize", mitsubishi1.get().rootId == carsId);
        final Optional<FundsMutationSubject> cars2 = Schema.FUNDS_MUTATION_SUBJECTS.findByName("Cars");
        assertTrue("Child flag did not update", cars2.get().childFlag);
    }

}