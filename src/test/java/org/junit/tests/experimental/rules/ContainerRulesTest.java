package org.junit.tests.experimental.rules;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Propagate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.rules.MethodRule;
import org.junit.rules.SuiteRule;
import org.junit.rules.SuiteWatchman;
import org.junit.rules.TestCaseRule;
import org.junit.rules.TestCaseWatchman;
import org.junit.rules.TestWatchman;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.FrameworkMethod;

public class ContainerRulesTest {

	private static LinkedList<Object> log= new LinkedList<Object>();

	@Before
	public void clearLog() {
		log.clear();
	}

	/**
	 * This tests that a SuiteRule can be added for a suite.
	 * @throws Exception
	 */
	@Test
	public void suiteRuleIsRunForSuite() throws Exception {
		JUnitCore.runClasses(SingleTestSuite.class);
		Iterator<Object> events= log.iterator();
		
		matchEventsToLog(
				"before suite" + SingleTestSuite.class,
				"test method executed",
				"after suite" + SingleTestSuite.class);
	}
	
	/**
	 * SuiteRule:s should be propagatable to sub suites (using the 
	 * @Propagate annotation), i.e. you only have to add them 
	 * at some root-level and they are applied on all sub-suites.
	 * @throws Exception
	 */
	@Test
	public void suiteRuleCanPropagateToNestedSuites() throws Exception {
		JUnitCore.runClasses(NestedSuite.class);
		Iterator<Object> events= log.iterator();

		matchEventsToLog(
				"before suite" + NestedSuite.class,
				"test method executed",
				"before suite" + LeafSuite.class,
				"test method executed",
				"after suite" + LeafSuite.class,
				"after suite" + NestedSuite.class);
	}

	/**
	 * You should be able to add a MethodRule at a root suite level
	 * and propagate it to all tests in the suite, including its 
	 * sub-suite.
	 * @throws Exception
	 */
	@Test
	public void methodRuleCanPropagateToNestedTestCases() throws Exception {
		JUnitCore.runClasses(PropagatingMethodRuleSuite.class);
		Iterator<Object> events= log.iterator();

		matchEventsToLog(
				"before method",
				"test method executed",
				"after method");
	}

	/**
	 * You should be able to add a TestCaseRule for test cases (i.e. classes)
	 * at a root suite level and have it applied to all classes in that suite.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCaseRuleOnSuiteIsRunForTest() throws Exception {
		JUnitCore.runClasses(TestCaseRuleSuite.class);

		matchEventsToLog(
				"before test case" + SimpleTestCase.class,
				"test method executed",
				"after test case" + SimpleTestCase.class);
	}

	/**
	 * You should be able to add a TestCaseRule for the executing class. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCaseRuleOnTestCaseIsRunForTest() throws Exception {
		JUnitCore.runClasses(LocalTestCaseRuleSuite.class);

		matchEventsToLog(
				"before test case" + TestCaseRuleTestCase.class,
				"test method executed",
				"after test case" + TestCaseRuleTestCase.class);
	}


	private void matchEventsToLog(String... expecteds) {
		String actual = "";
		String expected = "";
		for (Object entry : log) {
			actual += entry + "\n";
		}
		for (String expectedEntry : expecteds) {
			expected += expectedEntry + "\n";
		}
		assertEquals(expected, actual);
	}


	@RunWith(Suite.class)
	@SuiteClasses( { SimpleTestCase.class })
	public static class SingleTestSuite {
		// Either use @Rule or create a new annotation
		@Rule
		public static SuiteRule suiteRule= new SuiteWatchman() {
			@Override
			public void startingSuite(final Class<?> container) {
				log.add("before suite" + container);
			}
			@Override
			public void finishedSuite(final Class<?> container) {
				log.add("after suite" + container);
			}
		};
	}

	@RunWith(Suite.class)
	@SuiteClasses( { SimpleTestCase.class })
	public static class TestCaseRuleSuite {
		@Rule @Propagate
		public static TestCaseRule testCaseRule= new TestCaseWatchman() {
			@Override
			public void startingTestCase(final Class<?> testCase) {
				log.add("before test case" + testCase);
			}
			@Override
			public void finishedTestCase(final Class<?> testCase) {
				log.add("after test case" + testCase);
			}
		};
	}

	public static class SimpleTestCase {
		@Test
		public void method() throws Exception {
			log.add("test method executed");
		}
	}

	@RunWith(Suite.class)
	@SuiteClasses( { TestCaseRuleTestCase.class })
	public static class LocalTestCaseRuleSuite {
	}

	public static class TestCaseRuleTestCase {
		@Rule @Propagate
		public static TestCaseRule testCaseRule= new TestCaseWatchman() {
			@Override
			public void startingTestCase(final Class<?> testCase) {
				log.add("before test case" + testCase);
			}
			@Override
			public void finishedTestCase(final Class<?> testCase) {
				log.add("after test case" + testCase);
			}
		};
		@Test
		public void method() throws Exception {
			log.add("test method executed");
		}
	}

	@RunWith(Suite.class)
	@SuiteClasses( { SimpleTestCase.class, LeafSuite.class })
	public static class NestedSuite {
		// It should be able to declare a rule in one place only
		// (in a Suite), and then it runs for all contained suites
		// Maybe this needs a separate annotation, like @Propagate
		// or an attribute on the annotation
		@Rule
		@Propagate
		public static SuiteRule suiteRule= new SuiteWatchman() {
			@Override
			public void startingSuite(Class<?> container) {
				log.add("before suite" + container);
			}
			@Override
			public void finishedSuite(Class<?> container) {
				log.add("after suite" + container);
			}
		};
	}

	@RunWith(Suite.class)
	@SuiteClasses( { SimpleTestCase.class })
	public static class LeafSuite {
	}

	@RunWith(Suite.class)
	@SuiteClasses( { SimpleTestCase.class })
	public static class PropagatingMethodRuleSuite {
		@Rule
		@Propagate
		public static MethodRule propagatingMethodRule= new TestWatchman() {
			@Override
			public void starting(FrameworkMethod method) {
				log.add("before method");
			}
			@Override
			public void finished(FrameworkMethod method) {
				log.add("after method");
			}
		};
	}

}
