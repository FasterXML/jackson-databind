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
package com.fasterxml.jackson.databind.util.clhm;

import static com.fasterxml.jackson.databind.util.clhm.ConcurrentLinkedHashMap.checkNotNull;

import java.io.Serializable;

/**
 * A common set of {@link Weigher} and {@link EntryWeigher} implementations.
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
     * A entry weigher backed by the specified weigher. The weight of the value
     * determines the weight of the entry.
     *
     * @param weigher the weigher to be "wrapped" in an entry weigher.
     * @return A entry weigher view of the specified weigher.
     */
    static <K, V> EntryWeigher<K, V> asEntryWeigher(
            final Weigher<? super V> weigher) {
        return (weigher == singleton())
                ? Weighers.<K, V>entrySingleton()
                : new EntryWeigherView<K, V>(weigher);
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

    /**
     * A weigher where a value has a weight of <tt>1</tt>. A map bounded with
     * this weigher will evict when the number of key-value pairs exceeds the
     * capacity.
     *
     * @return A weigher where a value takes one unit of capacity.
     */
    @SuppressWarnings({"cast", "unchecked"})
    static <V> Weigher<V> singleton() {
        return (Weigher<V>) SingletonWeigher.INSTANCE;
    }

    static final class EntryWeigherView<K, V> implements EntryWeigher<K, V>, Serializable {
        static final long serialVersionUID = 1;
        final Weigher<? super V> weigher;

        EntryWeigherView(Weigher<? super V> weigher) {
            checkNotNull(weigher);
            this.weigher = weigher;
        }

        @Override
        public int weightOf(K key, V value) {
            return weigher.weightOf(value);
        }
    }

    enum SingletonEntryWeigher implements EntryWeigher<Object, Object> {
        INSTANCE;

        @Override
        public int weightOf(Object key, Object value) {
            return 1;
        }
    }

    enum SingletonWeigher implements Weigher<Object> {
        INSTANCE;

        @Override
        public int weightOf(Object value) {
            return 1;
        }
    }
}
