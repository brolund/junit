package org.junit.tests;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.junit.notify.RunNotifier;
import org.junit.plan.Plan;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.extensions.Enclosed;
import org.junit.runner.extensions.Sorter;
import org.junit.runner.internal.ClassRunner;
import org.junit.runner.internal.InitializationError;
import org.junit.runner.internal.TestClassRunner;

@RunWith(Enclosed.class)
public class SortableTest {
	private static Comparator<Plan> forward() {
		return new Comparator<Plan>() {
			public int compare(Plan o1, Plan o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
	}
	
	private static Comparator<Plan> backward() {
		return new Comparator<Plan>() {
			public int compare(Plan o1, Plan o2) {
				return o2.getName().compareTo(o1.getName());
			}
		};
	}

	public static class TestClassRunnerIsSortable {
		private static String log = "";
		
		public static class SortMe {
			@Test public void a() { log += "a"; }
			@Test public void b() { log += "b"; }
			@Test public void c() { log += "c"; }
		}
		
		@Before public void resetLog() {
			log = "";
		}
		
		@Test public void sortingForwardWorksOnTestClassRunner() {
			Request forward = Request.aClass(SortMe.class).sortWith(forward());
			
			new JUnitCore().run(forward);
			assertEquals("abc", log);
		}

		@Test public void sortingBackwardWorksOnTestClassRunner() {
			Request backward = Request.aClass(SortMe.class).sortWith(backward());
			
			new JUnitCore().run(backward);
			assertEquals("cba", log);
		}
		
		@RunWith(Enclosed.class) 
		public static class Enclosing {
			public static class A {
				@Test public void a() { log += "Aa"; }
				@Test public void b() { log += "Ab"; }
				@Test public void c() { log += "Ac"; }				
			}
			public static class B {
				@Test public void a() { log += "Ba"; }
				@Test public void b() { log += "Bb"; }
				@Test public void c() { log += "Bc"; }								
			}
		}

		@Test public void sortingForwardWorksOnSuite() {
			Request forward = Request.aClass(Enclosing.class).sortWith(forward());
			
			new JUnitCore().run(forward);
			assertEquals("AaAbAcBaBbBc", log);
		}

		@Test public void sortingBackwardWorksOnSuite() {
			Request backward = Request.aClass(Enclosing.class).sortWith(backward());
			
			new JUnitCore().run(backward);
			assertEquals("BcBbBaAcAbAa", log);
		}

	}
	
	public static class UnsortableRunnersAreHandledWithoutCrashing {
		public static class UnsortableRunner extends ClassRunner {
			public UnsortableRunner(Class<? extends Object> klass) {
				super(klass);
			}
			
			@Override
			public Plan getPlan() {
				return null;
			}
			
			@Override
			public void run(RunNotifier notifier) {
			}		
		}
		
		@RunWith(UnsortableRunner.class)
		public static class Unsortable {
			@Test public void a() {}		
		}
		
		@Test public void unsortablesAreHandledWithoutCrashing() {
			Request unsorted = Request.aClass(Unsortable.class).sortWith(forward());
			new JUnitCore().run(unsorted);
		}
		
		@Test public void testClassRunnerCanBeWrappedAroundUnsortable() throws InitializationError {
			TestClassRunner runner = new TestClassRunner(Unsortable.class, new UnsortableRunner(Unsortable.class));
			runner.sort(new Sorter(forward()));
		}
	}
}