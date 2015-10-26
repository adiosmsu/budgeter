package ru.adios.budgeter.inmemrepo;

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationSubjectRepoTester;

/**
 * Date: 6/15/15
 * Time: 5:28 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectPseudoTableTest {

    private final FundsMutationSubjectRepoTester tester = new FundsMutationSubjectRepoTester(Schema.INSTANCE);

    @Test
    public void testAddSubject() throws Exception {
        tester.testAddSubject();
    }

    @Test
    public void testFindByParent() throws Exception {
        tester.testFindByParent();
    }

    @Test
    public void testFindById() throws Exception {
        tester.testFindById();
    }

    @Test
    public void testFindByName() throws Exception {
        tester.testFindByName();
    }

    @Test
    public void testUpdateChildFlag() throws Exception {
        tester.testUpdateChildFlag();
    }

    @Test
    public void testSearchByString() throws Exception {
        tester.testSearchByString();
    }

    @Test
    public void testRawAddition() throws Exception {
        tester.testRawAddition();
    }

}