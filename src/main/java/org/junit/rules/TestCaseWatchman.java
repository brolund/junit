package org.junit.rules;

import org.junit.runners.model.Statement;

/**
 * The TestCaseWatchman has methods that can be used instead of
 * the @BeforeClass and @AfterClass annotated methods on a test case.
 */
public class TestCaseWatchman implements TestCaseRule {

	public Statement apply(final Statement base, final Class<?> testCase) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				startingTestCase(testCase);
				base.evaluate();
				finishedTestCase(testCase);
			}
			
		};
	}

	/**
	 * Called before a test case is executed
	 * @param testCase
	 */
	public void startingTestCase(Class<?> testCase) {
	}

	/**
	 * Called after a test case is executed
	 * @param testCase
	 */
	public void finishedTestCase(Class<?> testCase) {
	}

}
