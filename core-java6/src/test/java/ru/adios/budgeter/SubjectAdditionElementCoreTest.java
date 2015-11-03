package ru.adios.budgeter;

import java8.util.Optional;
import org.junit.Test;
import ru.adios.budgeter.api.Bundle;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.api.data.FundsMutationSubject;
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
        testSubmitWith(Schema.INSTANCE, TestUtils.CASE_INNER);
        testSubmitWith(TestUtils.JDBC_BUNDLE, TestUtils.CASE_JDBC);
    }

    private void testSubmitWith(Bundle bundle, String caseName) throws Exception {
        caseName += ": ";
        bundle.clearSchema();

        final FundsMutationSubjectRepository subjRepo = bundle.fundsMutationSubjects();
        SubjectAdditionElementCore core = new SubjectAdditionElementCore(subjRepo);
        core.setName("");
        Submitter.Result<FundsMutationSubject> submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertTrue(isFieldInError(submit, SubjectAdditionElementCore.FIELD_NAME));

        core.setName(null);
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertTrue(isFieldInError(submit, SubjectAdditionElementCore.FIELD_NAME));

        core.setName("Еда");
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertFalse(isFieldInError(submit, SubjectAdditionElementCore.FIELD_NAME));

        core.setParentName("Test");
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertTrue(isFieldInError(submit, SubjectAdditionElementCore.FIELD_PARENT_NAME));

        core.setType(-1);
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertTrue(isFieldInError(submit, SubjectAdditionElementCore.FIELD_TYPE));

        core.setType(1000);
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertTrue(isFieldInError(submit, SubjectAdditionElementCore.FIELD_TYPE));

        core.setType(FundsMutationSubject.Type.PRODUCT.ordinal());
        submit = core.submit();
        assertTrue(submit.isSuccessful());
        //noinspection ConstantConditions
        final long id = submit.submitResult.id.getAsLong();

        Optional<FundsMutationSubject> subjOpt = subjRepo.findByName("Еда");
        assertTrue(subjOpt.isPresent());
        assertEquals(caseName + "Food fault", FundsMutationSubject.builder(subjRepo).setName("Еда").setType(FundsMutationSubject.Type.PRODUCT).build(), subjOpt.get());

        core = new SubjectAdditionElementCore(subjRepo);
        core.setName("Хлеб");
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertFalse(isFieldInError(submit, SubjectAdditionElementCore.FIELD_NAME));

        core.setParentName("Еда");
        submit = core.submit();
        assertFalse(submit.isSuccessful());
        assertFalse(isFieldInError(submit, SubjectAdditionElementCore.FIELD_PARENT_NAME));

        core.setType(FundsMutationSubject.Type.PRODUCT);
        submit = core.submit();
        assertTrue(submit.isSuccessful());

        subjOpt = subjRepo.findByName("Хлеб");
        assertTrue(subjOpt.isPresent());
        assertEquals(
                caseName + "Bread fault",
                FundsMutationSubject.builder(subjRepo)
                        .setName("Хлеб")
                        .setParentId(id)
                        .setRootId(id)
                        .setType(FundsMutationSubject.Type.PRODUCT)
                        .build(),
                subjOpt.get()
        );
    }

    private boolean isFieldInError(Submitter.Result<FundsMutationSubject> submit, String fieldName) {
        for (Submitter.FieldError fieldError : submit.fieldErrors) {
            if (fieldError.fieldInFault.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

}