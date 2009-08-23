package org.junit.rules;

import org.junit.runners.model.Statement;

/**
 * The SuiteWatchman has methods that can be used instead of
 * the @BeforeClass and @AfterClass annotated methods on a suite.
 */
public class SuiteWatchman extends SuiteRule {

	@Override
	public Statement apply(final Statement base, final Class<?> suiteClass) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				startingSuite(suiteClass);
				base.evaluate();
				finishedSuite(suiteClass);
			}
		};
	}

	/**
	 * Called before a test suite begins, or before a test case (class) is executed.
	 * @param suiteClass The test suite class
	 */
	public void startingSuite(Class<?> suiteClass) {
	}

	/**
	 * Called after a test suite is finished.
	 * @param suiteClass The test suite class
	 */
	public void finishedSuite(Class<?> suiteClass) {
	}

}
