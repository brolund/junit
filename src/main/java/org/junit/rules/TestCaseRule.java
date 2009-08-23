package org.junit.rules;

import org.junit.runners.model.Statement;

/**
 * A TestCaseRule is at the execution of a test case, i.e. a test class.
 */
public interface TestCaseRule {
	/**
	 * This method is called when executing test case.
	 * 
	 * @param base
	 *            The original test case statement to execute
	 * @param aggregator
	 *            The class to execute
	 * @return A new statement that applies any additional logic to
	 *         executing the suite or the test case
	 */
	Statement apply(Statement base, Class<?> testCase);
}
