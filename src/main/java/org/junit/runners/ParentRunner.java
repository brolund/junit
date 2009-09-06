package org.junit.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Propagate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.SuiteRule;
import org.junit.rules.TestCaseRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.tests.experimental.rules.ContainerRulesTest;
import org.omg.CosNaming.IstringHelper;

/**
 * Provides most of the functionality specific to a Runner that implements a
 * "parent node" in the test tree, with children defined by objects of some data
 * type {@code T}. (For {@link BlockJUnit4ClassRunner}, {@code T} is
 * {@link Method} . For {@link Suite}, {@code T} is {@link Class}.) Subclasses
 * must implement finding the children of the node, describing each child, and
 * running each child. ParentRunner will filter and sort children, handle
 * {@code @BeforeClass} and {@code @AfterClass} methods, create a composite
 * {@link Description}, and run children sequentially.
 */
public abstract class ParentRunner<T> extends Runner implements Filterable,
		Sortable {
	private final TestClass fTestClass;

	private Filter fFilter= null;

	private Sorter fSorter= Sorter.NULL;

	private RunnerScheduler fScheduler= new RunnerScheduler() {	
		public void schedule(Runnable childStatement) {
			childStatement.run();
		}
	
		public void finished() {
			// do nothing
		}
	};

	/**
	 * Constructs a new {@code ParentRunner} that will run {@code @TestClass}
	 * @throws InitializationError 
	 */
	protected ParentRunner(Class<?> testClass) throws InitializationError {
		fTestClass= new TestClass(testClass);
		validate();
	}

	//
	// Must be overridden
	//

	/**
	 * Returns a list of objects that define the children of this Runner.
	 */
	protected abstract List<T> getChildren();

	/**
	 * Returns a {@link Description} for {@code child}, which can be assumed to
	 * be an element of the list returned by {@link ParentRunner#getChildren()}
	 */
	protected abstract Description describeChild(T child);

	/**
	 * Runs the test corresponding to {@code child}, which can be assumed to be
	 * an element of the list returned by {@link ParentRunner#getChildren()}.
	 * Subclasses are responsible for making sure that relevant test events are
	 * reported through {@code notifier}
	 */
	protected abstract void runChild(T child, RunNotifier notifier);
		
	//
	// May be overridden
	//
	
	/**
	 * Adds to {@code errors} a throwable for each problem noted with the test class (available from {@link #getTestClass()}).
	 * Default implementation adds an error for each method annotated with
	 * {@code @BeforeClass} or {@code @AfterClass} that is not
	 * {@code public static void} with no arguments.
	 */
	protected void collectInitializationErrors(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(BeforeClass.class, true, errors);
		validatePublicVoidNoArgMethods(AfterClass.class, true, errors);
	}

	/**
	 * Adds to {@code errors} if any method in this class is annotated with
	 * {@code annotation}, but:
	 * <ul>
	 * <li>is not public, or
	 * <li>takes parameters, or
	 * <li>returns something other than void, or
	 * <li>is static (given {@code isStatic is false}), or
	 * <li>is not static (given {@code isStatic is true}).
	 */
	protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation,
			boolean isStatic, List<Throwable> errors) {
		List<FrameworkMethod> methods= getTestClass().getAnnotatedMethods(annotation);

		for (FrameworkMethod eachTestMethod : methods)
			eachTestMethod.validatePublicVoidNoArg(isStatic, errors);
	}

	/** 
	 * Constructs a {@code Statement} to run all of the tests in the test class. Override to add pre-/post-processing. 
	 * Here is an outline of the implementation:
	 * <ul>
	 * <li>Call {@link #runChild(Object, RunNotifier)} on each object returned by {@link #getChildren()} (subject to any imposed filter and sort).</li>
	 * <li>ALWAYS run all non-overridden {@code @BeforeClass} methods on this class
	 * and superclasses before the previous step; if any throws an
	 * Exception, stop execution and pass the exception on.
	 * <li>ALWAYS run all non-overridden {@code @AfterClass} methods on this class
	 * and superclasses before any of the previous steps; all AfterClass methods are
	 * always executed: exceptions thrown by previous steps are combined, if
	 * necessary, with exceptions from AfterClass methods into a
	 * {@link MultipleFailureException}.
	 * </ul>
	 * TODO: Needs updating if rules implementation is accepted
	 * @param notifier
	 * @return {@code Statement}
	 */
	protected Statement classBlock(final RunNotifier notifier) {
		Statement statement= childrenInvoker(notifier);
		statement= withBeforeClasses(statement);
		statement= withAfterClasses(statement);
		statement= withRules(statement, notifier);
		return statement;
	}

	/**
	 * Appends TestCaseRules and SuiteRules to the statement
	 * @param statement
	 * @param notifier
	 * @return
	 */
	private Statement withRules(final Statement statement, RunNotifier notifier) {
		final List<Object> ancestory = notifier.getAncestoryRootToLeafCopy();
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				Statement localStatement = statement;
				localStatement= withLocalTestCaseRules(localStatement);
				localStatement= withPropagatedTestCaseRules(ancestory, localStatement);
				localStatement= withSuiteRules(ancestory, localStatement);
				localStatement.evaluate();
			}

		};
	}
	
	/**
	 * Appends propagated and local suite rules if the current class is a
	 * suite
	 * @param ancestory
	 * @param statement
	 * @return
	 */
	private Statement withSuiteRules(final List<Object> ancestory,
			Statement statement) {
		for (Object ancestor : ancestory) {
			if(ancestor instanceof Suite && !isTestCase(fTestClass)) {
				TestClass testClass= ((Suite) ancestor).getTestClass();
				if(isTestCase(testClass)) continue;
				
				List<SuiteRule> rules = getSuiteRules(testClass, fTestClass);
				for (SuiteRule suiteRule : rules) {
					statement = suiteRule.apply(statement, fTestClass.getJavaClass());
				}
				
			}
		}
		return statement;
	}

	/**
	 * Appends propagated TestCaseRules if the current class is a 
	 * test case.
	 * @param ancestory
	 * @param localStatement
	 * @return
	 * @throws IllegalAccessException
	 */
	private Statement withPropagatedTestCaseRules(
			final List<Object> ancestory, Statement localStatement)
			throws IllegalAccessException {
		for (Object ancestor : ancestory) {
			if(ancestor instanceof TestClass) {
				List<TestCaseRule> rules= getTestCaseRules((TestClass) ancestor);
				for (TestCaseRule testCaseRule : rules) {
					localStatement = testCaseRule.apply(localStatement, fTestClass.getJavaClass());
				}
			}
		}
		return localStatement;
	}

	/**
	 * Appends local TestCaseRules if the current fTestClass is 
	 * a test case.
	 * @param localStatement
	 * @return
	 * @throws IllegalAccessException
	 */
	private Statement withLocalTestCaseRules(Statement localStatement)
			throws IllegalAccessException {
		if(isTestCase(fTestClass)) {
			List<TestCaseRule> rules= getTestCaseRules(fTestClass);
			for (TestCaseRule testCaseRule : rules) {
				localStatement = testCaseRule.apply(localStatement, fTestClass.getJavaClass());
			}
		}
		return localStatement;
	}

	/**
	 * Provides SuiteRules for a TestClass. Rules returned are either Propagated 
	 * (using the Propagate annotation) or local to the current suite.
	 * @param ruleProvider
	 * @param currentClass
	 * @return
	 */
	private List<SuiteRule> getSuiteRules(TestClass ruleProvider, TestClass currentClass) {
		List<SuiteRule> rules = new LinkedList<SuiteRule>();
		List<FrameworkField> annotatedFields= ruleProvider.getAnnotatedFields(Rule.class);

		for (FrameworkField frameworkField : annotatedFields) {
			Class<?> type= frameworkField.getField().getType();
			if(type.isAssignableFrom(SuiteRule.class) && 
				(frameworkField.getField().isAnnotationPresent(Propagate.class) ||
				currentClass.getJavaClass().equals(ruleProvider.getJavaClass()))) {
				SuiteRule suiteRule = getSuiteRule(frameworkField);
				rules.add(suiteRule);
			}
		}
		return rules;
	}

	/**
	 * Method to avoid non-applicable exeption
	 */
	private SuiteRule getSuiteRule(FrameworkField frameworkField) {
		try {
			return (SuiteRule) frameworkField.get(null);
		} catch (Exception e) {
			throw new RuntimeException("SuiteRule should exists here.", e);
		}
	}

	/**
	 * Returns the TestCaseRules declared on the TestClass
	 * @param testClass
	 * @return
	 * @throws IllegalAccessException
	 */
	private List<TestCaseRule> getTestCaseRules(TestClass testClass)
			throws IllegalAccessException {
		List<FrameworkField> annotatedFields= testClass.getAnnotatedFields(Rule.class);
		List<TestCaseRule> rules = new LinkedList<TestCaseRule>();
		for (FrameworkField frameworkField : annotatedFields) {
			Class<?> type= frameworkField.getField().getType();
			if(type.isAssignableFrom(TestCaseRule.class)) {
				TestCaseRule testCaseRule = (TestCaseRule) frameworkField.get(null);
				rules.add(testCaseRule);
			}
		}
		return rules;
	}

	/**
	 * Returns true if testClass contains methods annotated with @Test, false otherwise.
	 * @param testClass
	 * @return
	 */
	private boolean isTestCase(TestClass testClass) {
		return !testClass.getAnnotatedMethods(Test.class).isEmpty();
	}


	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @BeforeClass} methods on this class
	 * and superclasses before executing {@code statement}; if any throws an
	 * Exception, stop execution and pass the exception on.
	 */
	protected Statement withBeforeClasses(Statement statement) {
		List<FrameworkMethod> befores= fTestClass
				.getAnnotatedMethods(BeforeClass.class);
		return befores.isEmpty() ? statement :
			new RunBefores(statement, befores, null);
	}

	/**
	 * Returns a {@link Statement}: run all non-overridden {@code @AfterClass} methods on this class
	 * and superclasses before executing {@code statement}; all AfterClass methods are
	 * always executed: exceptions thrown by previous steps are combined, if
	 * necessary, with exceptions from AfterClass methods into a
	 * {@link MultipleFailureException}.
	 */
	protected Statement withAfterClasses(Statement statement) {
		List<FrameworkMethod> afters= fTestClass
				.getAnnotatedMethods(AfterClass.class);
		return afters.isEmpty() ? statement : 
			new RunAfters(statement, afters, null);
	}

	/**
	 * Returns a {@link Statement}: Call {@link #runChild(Object, RunNotifier)}
	 * on each object returned by {@link #getChildren()} (subject to any imposed
	 * filter and sort)
	 */
	protected Statement childrenInvoker(final RunNotifier notifier) {
		return new Statement() {
			@Override
			public void evaluate() {
				notifier.pushAncestor(fTestClass);
				runChildren(notifier);
				notifier.popAncestor();
			}
		};
	}

	private void runChildren(final RunNotifier notifier) {
		for (final T each : getFilteredChildren())
			fScheduler.schedule(new Runnable() {			
				public void run() {
					notifier.pushAncestor(each);
					ParentRunner.this.runChild(each, notifier);
					notifier.popAncestor();
				}
			});
		fScheduler.finished();
	}

	/**
	 * Returns a name used to describe this Runner
	 */
	protected String getName() {
		return fTestClass.getName();
	}

	//
	// Available for subclasses
	//

	/**
	 * Returns a {@link TestClass} object wrapping the class to be executed.
	 */
	public final TestClass getTestClass() {
		return fTestClass;
	}

	//
	// Implementation of Runner
	// 
	
	@Override
	public Description getDescription() {
		Description description= Description.createSuiteDescription(getName(),
				fTestClass.getAnnotations());
		for (T child : getFilteredChildren())
			description.addChild(describeChild(child));
		return description;
	}

	@Override
	public void run(final RunNotifier notifier) {
		EachTestNotifier testNotifier= new EachTestNotifier(notifier,
				getDescription());
		try {
			Statement statement= classBlock(notifier);
			statement.evaluate();
		} catch (AssumptionViolatedException e) {
			testNotifier.fireTestIgnored();
		} catch (StoppedByUserException e) {
			throw e;
		} catch (Throwable e) {
			testNotifier.addFailure(e);
		}
	}
	
	//
	// Implementation of Filterable and Sortable
	//

	public void filter(Filter filter) throws NoTestsRemainException {
		fFilter= filter;

		for (T each : getChildren())
			if (shouldRun(each))
				return;
		throw new NoTestsRemainException();
	}

	public void sort(Sorter sorter) {
		fSorter= sorter;
	}
	
	//
	// Private implementation
	// 

	private void validate() throws InitializationError {
		List<Throwable> errors= new ArrayList<Throwable>();
		collectInitializationErrors(errors);
		if (!errors.isEmpty())
			throw new InitializationError(errors);
	}

	private List<T> getFilteredChildren() {
		ArrayList<T> filtered= new ArrayList<T>();
		for (T each : getChildren())
			if (shouldRun(each))
				try {
					filterChild(each);
					sortChild(each);
					filtered.add(each);
				} catch (NoTestsRemainException e) {
					// don't add it
				}
		Collections.sort(filtered, comparator());
		return filtered;
	}

	private void sortChild(T child) {
		fSorter.apply(child);
	}

	private void filterChild(T child) throws NoTestsRemainException {
		if (fFilter != null)
			fFilter.apply(child);
	}

	private boolean shouldRun(T each) {
		return fFilter == null || fFilter.shouldRun(describeChild(each));
	}

	private Comparator<? super T> comparator() {
		return new Comparator<T>() {
			public int compare(T o1, T o2) {
				return fSorter.compare(describeChild(o1), describeChild(o2));
			}
		};
	}

	/**
	 * Sets a scheduler that determines the order and parallelization
	 * of children.  Highly experimental feature that may change.
	 */
	public void setScheduler(RunnerScheduler scheduler) {
		this.fScheduler = scheduler;
	}
}