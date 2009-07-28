package org.junit.tests.experimental.rules;

import static org.junit.Assert.*;
import java.util.Iterator;
import java.util.LinkedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.Statement;


public class ContainerRuleTest {

    private static LinkedList<Object> log = new LinkedList<Object >();
    
    @Test
    public void containerRuleIsRunForSuite() throws Exception {
        log.clear();
        JUnitCore.runClasses(ContainerRuleSuite.class);
        Iterator<Object> events = log.iterator();

        assertEquals("before suite execution", events.next());
        assertEquals(ContainerRuleSuite.class, events.next());
        assertEquals("test method executed", events.next());
        assertEquals("after suite execution", events.next());
    }
    
    @RunWith(Suite.class)
    @SuiteClasses({SimpleTestCase.class})
    public static class ContainerRuleSuite {

        @Rule //Either use @Rule or create a new annotation
        public ContainerRule containerRule = new ContainerRule() {
            @Override 
            public Statement apply(final Statement base, final Class<?> container) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        log.push("before suite execution");
                        log.push(container);
                        base.evaluate();
                        log.push("after suite execution");
                    }
                };
            }};
    }

    /**
     * Rule for classes containing Statements to evaluate. Examples
     * are suites that contain suites or test cases, or tests that 
     * contain test methods.
     */
    public static abstract class ContainerRule {
        /**
         * This method is called when executing a suite or a test case.
         * @param base The original statement to execute (a suite or a test case)
         * @param aggregator The suite or the class to execute
         * @return A new statement that applies any additional logic to 
         *         executing the suite or the test case
         */
        public abstract Statement apply(Statement base, Class<?> container);
    }

    public static class SimpleTestCase {
        @Test
        public void method() throws Exception {
            log.add("test method executed");
        }
    }

     
}
