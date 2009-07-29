package org.junit.tests.experimental.rules;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;


public class ContainerRuleTest {

    private static LinkedList<Object> log = new LinkedList<Object >();
    
    @Before
    public void clearLog() {
    	log.clear();
    }
    
    @Test
    public void containerRuleIsRunForSuite() throws Exception {
        JUnitCore.runClasses(SingleTestSuite.class);
        Iterator<Object> events = log.iterator();

        assertEquals("before suite execution", events.next());
        assertEquals(SingleTestSuite.class, events.next());
        assertEquals("test method executed", events.next());
        assertEquals("after suite execution", events.next());
    }

    @Test
    public void containerRuleCanPropagateToNestedSuites() throws Exception {
        JUnitCore.runClasses(NestedSuite.class);
        Iterator<Object> events = log.iterator();

        assertEquals("before suite execution", events.next());
        assertEquals(NestedSuite.class, events.next());
        assertEquals("test method executed", events.next());
        assertEquals("before suite execution", events.next());
        assertEquals(LeafSuite.class, events.next());
        assertEquals("test method executed", events.next());
        assertEquals("after suite execution", events.next());
        assertEquals("after suite execution", events.next());
    }

    @Test
    public void methodRuleCanPropagateToNestedTestCases() throws Exception {
        JUnitCore.runClasses(PropagatingMethodRuleSuite.class);
        Iterator<Object> events = log.iterator();

        assertEquals("before method", events.next());
        assertEquals("test method executed", events.next());
        assertEquals("after method", events.next());
    }

    private static class LoggingContainerRule extends ContainerRule {
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
        }
	}
    
	private static class LoggingMethodRule implements MethodRule {
		public Statement apply(final Statement base, FrameworkMethod method, Object target) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					log.add("before method");
					base.evaluate();
					log.add("after method");
				}
				
			};
		}
	}


    @RunWith(Suite.class)
    @SuiteClasses({SimpleTestCase.class})
    public static class SingleTestSuite {
		@Rule //Either use @Rule or create a new annotation
        public ContainerRule containerRule = new LoggingContainerRule();
    }

    public static class SimpleTestCase {
        @Test
        public void method() throws Exception {
            log.add("test method executed");
        }
    }

    @RunWith(Suite.class)
    @SuiteClasses({SimpleTestCase.class, LeafSuite.class})
    public static class NestedSuite {
    	// It should be able to declare a rule in one place only 
    	// (in a Suite), and then it runs for all contained suites
    	// Maybe this needs a separate annotation, like @Propagate
    	// or an attribute on the annotation
		@Rule @Propagate
        public ContainerRule containerRule = new LoggingContainerRule();
    }

    @RunWith(Suite.class)
    @SuiteClasses({SimpleTestCase.class})
    public static class LeafSuite {
    }

    @RunWith(Suite.class)
    @SuiteClasses({SimpleTestCase.class})
    public static class PropagatingMethodRuleSuite {

		@Rule @Propagate
        public MethodRule propagatingMethodRule = new LoggingMethodRule();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Propagate {}
    
    /**
     * Rule for classes containing Statements to evaluate. Examples
     * are suites that contain suites or test cases, or test cases that 
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

     
}
