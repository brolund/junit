package org.junit.tests;

import org.junit.Propagate;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.SuiteRule;
import org.junit.rules.SuiteWatchman;
import org.junit.rules.TestCaseRule;
import org.junit.rules.TestCaseWatchman;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.FrameworkMethod;


@RunWith(Suite.class)
@SuiteClasses({AllTests.class})
public class Documentation {
	public static String document = "";
	
	@Rule @Propagate
	public static SuiteRule propagatingSuite = new SuiteWatchman() {
		@Override
		public void startingSuite(Class<?> suiteClass) {
			document += "\n<h1>" + suiteClass.getSimpleName() + "</h1>";
		}
		@Override
		public void finishedSuite(Class<?> suiteClass) {
			document += "\n";
		}
	};
	@Rule
	public static SuiteRule documentWatchman = new SuiteWatchman() {
		@Override
		public void startingSuite(Class<?> suiteClass) {
			document += "<html><body>";
		}
		@Override
		public void finishedSuite(Class<?> suiteClass) {
			document += "\n</body></html>";
			System.out.println(document);
		}
	};

	@Rule @Propagate
	public static TestCaseRule testcaseWatchman = new TestCaseWatchman() {
		@Override
		public void startingTestCase(Class<?> testCase) {
			document += "\n<h2>" + testCase.getSimpleName() + "</h2>";
		}
		@Override
		public void finishedTestCase(Class<?> testCase) {
			document += "\n";
		}
	};

	@Rule @Propagate
	public static MethodRule methodWatchman = new TestWatchman() {
		
		@Override
		public void failed(Throwable e, FrameworkMethod method) {
			document += "\nFailure!!!";
		}
		@Override
		public void finished(FrameworkMethod method) {
			document += "\n";
		}
		@Override
		public void starting(FrameworkMethod method) {
			document += "\n<h3>" + method.getName() + "</h3>";
		}
		@Override
		public void succeeded(FrameworkMethod method) {
			document += "\nSuccess!!!";
		}
	};
	
}
