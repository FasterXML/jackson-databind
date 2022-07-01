package com.fasterxml.jackson.databind.util.internal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public final class CLHMTestlibTests extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        addCLHMViewTests(suite);
        return suite;
    }

    private static void addCLHMViewTests(TestSuite suite) {
        suite.addTest(MapTestFactory.suite("CLHMView", MapTestFactory.synchronousGenerator(
                () -> new PrivateMaxEntriesMap.Builder<String, String>()
                    .maximumCapacity(10).build()))
        );
    }
}
