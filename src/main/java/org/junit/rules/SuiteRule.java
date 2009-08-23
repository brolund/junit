package org.junit.rules;

import org.junit.runners.model.Statement;

/**
 * A SuiteRule is at the execution of a suite.
 */
public abstract class SuiteRule {
	/**
	 * This method is called when executing a suite.
	 * 
	 * @param base
	 *            The original suite to execute
	 * @param suite
	 *            The suite class about to be executed
	 * @return A new statement that applies any additional logic to
	 *         executing the suite or the test case
	 */
	public abstract Statement apply(Statement base, Class<?> suite);
}