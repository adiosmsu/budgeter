package ru.adios.budgeter.inmemrepo;

import org.junit.Before;
import org.junit.Test;
import ru.adios.budgeter.api.SubjectPriceRepoTester;

/**
 * Date: 11/9/15
 * Time: 12:30 PM
 *
 * @author Mikhail Kulikov
 */
public class SubjectPricePseudoTableTest {

    private final SubjectPriceRepoTester tester = new SubjectPriceRepoTester(Schema.INSTANCE);

    @Before
    public void setUp() {
        tester.setUp();
    }

    @Test
    public void testCountByAgent() throws Exception {
        tester.testCountByAgent();
    }

    @Test
    public void testStream() throws Exception {
        tester.testStream();
    }

    @Test
    public void testStreamByAgent() throws Exception {
        tester.testStreamByAgent();
    }

    @Test
    public void testRegister() throws Exception {
        tester.testRegister();
    }

    @Test
    public void testPriceExists() throws Exception {
        tester.testPriceExists();
    }

    @Test
    public void testCount() throws Exception {
        tester.testCount();
    }

}
