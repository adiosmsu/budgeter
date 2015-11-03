package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationSubjectRepoTester;

/**
 * Date: 6/15/15
 * Time: 5:28 PM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationSubjectJdbcRepositoryTest {

    private final FundsMutationSubjectRepoTester tester = new FundsMutationSubjectRepoTester(TestContext.BUNDLE);

    @Test
    public void testRawAddition() throws Exception {
        TestContext.ex(tester::testRawAddition);
    }

    @Test
    public void testUpdateChildFlag() throws Exception {
        TestContext.ex(tester::testUpdateChildFlag);
    }

    @Test
    public void testFindById() throws Exception {
        TestContext.ex(tester::testFindById);
    }

    @Test
    public void testFindByName() throws Exception {
        TestContext.ex(tester::testFindByName);
    }

    @Test
    public void testFindByParent() throws Exception {
        TestContext.ex(tester::testFindByParent);
    }

    @Test
    public void testSearchByString() throws Exception {
        TestContext.ex(tester::testSearchByString);
    }

    @Test
    public void testAddSubject() throws Exception {
        TestContext.ex(tester::testAddSubject);
    }

}