/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package com.fasterxml.jackson.databind.util.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

//copied from https://github.com/apache/cayenne/blob/b156addac1c8e4079fa88e977fee609210c5da69/cayenne-server/src/test/java/org/apache/cayenne/util/concurrentlinkedhashmap/ConcurrentLinkedHashMapTest.java
public class PrivateMaxEntriesMapTest {

    @Test
    public void testPutGet() {
        PrivateMaxEntriesMap<String, Object> m = new PrivateMaxEntriesMap.Builder<String, Object>()
                .maximumCapacity(10).build();

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        assertNull(m.get("nosuchkey"));
        assertEquals(100, m.get("k1"));

        m.put("k2", 200);
        assertEquals(2, m.size());
        assertEquals(200, m.get("k2"));
    }

    @Test
    public void testLRU() {
        PrivateMaxEntriesMap<String, Object> m = new PrivateMaxEntriesMap.Builder<String, Object>()
                .maximumCapacity(5).build();

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        m.put("k2", 101);
        assertEquals(2, m.size());
        m.put("k3", 102);
        assertEquals(3, m.size());
        m.put("k4", 103);
        assertEquals(4, m.size());
        m.put("k5", 104);
        assertEquals(5, m.size());
        m.put("k6", 105);
        assertEquals(5, m.size());
        m.put("k7", 106);
        assertEquals(5, m.size());
        m.put("k8", 107);
        assertEquals(5, m.size());

        m.remove("k6");
        assertEquals(4, m.size());

    }
}
