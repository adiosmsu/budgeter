package ru.adios.budgeter.jdbcrepo;

import org.junit.Test;
import ru.adios.budgeter.api.FundsMutationAgentRepoTester;

/**
 * Date: 7/1/15
 * Time: 6:55 AM
 *
 * @author Mikhail Kulikov
 */
public class FundsMutationAgentJdbcRepositoryTest {

    private FundsMutationAgentRepoTester tester = new FundsMutationAgentRepoTester(TestContext.BUNDLE);

    @Test
    public void testAddAgent() throws Exception {
        TestContext.ex(tester::testAddAgent);
    }

}