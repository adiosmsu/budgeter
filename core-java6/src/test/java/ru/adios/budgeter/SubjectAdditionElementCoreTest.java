package ru.adios.budgeter;

import java8.util.Optional;
import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.inmemrepo.Schema;

import static org.junit.Assert.*;

/**
 * Date: 7/12/15
 * Time: 5:30 AM
 *
 * @author Mikhail Kulikov
 */
public class SubjectAdditionElementCoreTest {

    @Test
    public void testSubmit() throws Exception {
        Schema.clearSchema();

        final long id = Schema.FUNDS_MUTATION_SUBJECTS.idSeqNext() + 1;
        final FundsMutationSubjectRepositoryMock subjRepo = new FundsMutationSubjectRepositoryMock();
        SubjectAdditionElementCore core = new SubjectAdditionElementCore(subjRepo);
        boolean nameSet = core.setName(Optional.of(""));
        assertFalse(nameSet);
        nameSet = core.setName(Optional.<String>empty());
        assertFalse(nameSet);
        nameSet = core.setName(Optional.of("Еда"));
        assertTrue(nameSet);
        boolean parentSet = core.setParentName("Test");
        assertFalse(parentSet);
        boolean typeSet = core.setType(-1);
        assertFalse(typeSet);
        typeSet = core.setType(1000);
        assertFalse(typeSet);
        typeSet = core.setType(FundsMutationSubject.Type.PRODUCT.ordinal());
        assertTrue(typeSet);
        core.submit();

        Optional<FundsMutationSubject> subjOpt = subjRepo.findByName("Еда");
        assertTrue(subjOpt.isPresent());
        assertEquals("Food fault", FundsMutationSubject.builder(subjRepo).setName("Еда").setType(FundsMutationSubject.Type.PRODUCT).build(), subjOpt.get());

        core = new SubjectAdditionElementCore(subjRepo);
        nameSet = core.setName(Optional.of("Хлеб"));
        assertTrue(nameSet);
        parentSet = core.setParentName("Еда");
        assertTrue(parentSet);
        core.setType(FundsMutationSubject.Type.PRODUCT);
        core.submit();

        subjOpt = subjRepo.findByName("Хлеб");
        assertTrue(subjOpt.isPresent());
        assertEquals(
                "Bread fault",
                FundsMutationSubject.builder(subjRepo)
                        .setName("Хлеб")
                        .setParentId((int) id)
                        .setRootId((int) id)
                        .setType(FundsMutationSubject.Type.PRODUCT)
                        .build(),
                subjOpt.get()
        );
    }

}