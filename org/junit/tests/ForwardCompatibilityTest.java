package org.junit.tests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestResult;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ForwardCompatibilityTest extends TestCase {
	static String fLog;
	
	static public class NewTest {
		@Before public void before() { 
			fLog+= "before "; 
		}
		@After public void after() {
			fLog+= "after ";
		}
		@Test public void test() {
			fLog+= "test ";
		}
	}
	
	public void testCompatibility() {
		fLog= "";
		TestResult result= new TestResult();
		junit.framework.Test adapter= new JUnit4TestAdapter(NewTest.class);
		adapter.run(result);
		assertEquals("before test after ", fLog);
	}
	
	static Exception exception= new Exception();
	
	public static class ErrorTest {
		@Test public void error() throws Exception {
			throw exception;
		}
	}
	public void testException() {
		TestResult result= new TestResult();
		junit.framework.Test adapter= new JUnit4TestAdapter(ErrorTest.class);
		adapter.run(result);
		assertEquals(exception, result.errors().get(0).thrownException());
	}
	
	public static class NoExceptionTest {
		@Test(expected=Exception.class) public void succeed() throws Exception {
		}
	}
	public void testNoException() {
		TestResult result= new TestResult();
		junit.framework.Test adapter= new JUnit4TestAdapter(NoExceptionTest.class);
		adapter.run(result);
		assertFalse(result.wasSuccessful());
	}
	
	public static class ExpectedTest {
		@Test(expected = Exception.class) public void expected() throws Exception {
			throw new Exception();
		}
	}
	public void testExpected() {
		TestResult result= new TestResult();
		junit.framework.Test adapter= new JUnit4TestAdapter(ExpectedTest.class);
		adapter.run(result);
		assertTrue(result.wasSuccessful());
	}
	
	static String log;
	public static class BeforeClassTest {
		@BeforeClass public static void beforeClass() {
			log+= "before class ";
		}
		@Before public void before() {
			log+= "before ";
		}
		@Test public void one() {
			log+= "test ";
		}
		@Test public void two()  {
			log+= "test ";
		}
		@After public void after() {
			log+= "after ";
		}
		@AfterClass public static void afterClass() {
			log+= "after class ";
		}
	}
	
	public void testBeforeAndAfterClass() {
		log= "";
		TestResult result= new TestResult();
		junit.framework.Test adapter= new JUnit4TestAdapter(BeforeClassTest.class);
		adapter.run(result);
		assertEquals("before class before test after before test after after class ", log);
	}
}