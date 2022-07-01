/*
 * Copyright 2010 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fasterxml.jackson.databind.util.internal;

import static com.fasterxml.jackson.databind.util.internal.PrivateMaxEntriesMap.checkNotNull;

import java.io.Serializable;

/**
 * A common set of {@link EntryWeigher} implementations.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 * @see <a href="http://code.google.com/p/concurrentlinkedhashmap/">
 *      http://code.google.com/p/concurrentlinkedhashmap/</a>
 */
final class Weighers {

    private Weighers() {
        throw new AssertionError();
    }

    /**
     * A weigher where an entry has a weight of <tt>1</tt>. A map bounded with
     * this weigher will evict when the number of key-value pairs exceeds the
     * capacity.
     *
     * @return A weigher where a value takes one unit of capacity.
     */
    @SuppressWarnings({"cast", "unchecked"})
    static <K, V> EntryWeigher<K, V> entrySingleton() {
        return (EntryWeigher<K, V>) SingletonEntryWeigher.INSTANCE;
    }

    enum SingletonEntryWeigher implements EntryWeigher<Object, Object> {
        INSTANCE;

        @Override
        public int weightOf(Object key, Object value) {
            return 1;
        }
    }
}
