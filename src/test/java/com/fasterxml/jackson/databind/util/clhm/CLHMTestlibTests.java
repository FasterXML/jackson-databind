package com.fasterxml.jackson.databind.util.clhm;

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
                () -> new ConcurrentLinkedHashMap.Builder<String, String>()
                    .maximumWeightedCapacity(10).build()))
        );
    }
}
