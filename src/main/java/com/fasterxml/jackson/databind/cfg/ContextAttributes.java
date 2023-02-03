package com.fasterxml.jackson.databind.cfg;

import java.util.*;

/**
 * Helper class used for storing and accessing per-call attributes.
 * Storage is two-layered: at higher precedence, we have actual per-call
 * attributes; and at lower precedence, default attributes that may be
 * defined for Object readers and writers.
 *<p>
 * Note that the way mutability is implemented differs between kinds
 * of attributes, to account for thread-safety: per-call attributes
 * are handled assuming that instances are never shared, whereas
 * changes to per-reader/per-writer attributes are made assuming
 * sharing, by creating new copies instead of modifying state.
 * This allows sharing of default values without per-call copying, but
 * requires two-level lookup on access.
 *
 * @since 2.3
 */
public abstract class ContextAttributes
{
    public static ContextAttributes getEmpty() {
        return Impl.getEmpty();
    }

    /*
    /**********************************************************
    /* Per-reader/writer access
    /**********************************************************
     */

    public abstract ContextAttributes withSharedAttribute(Object key, Object value);

    public abstract ContextAttributes withSharedAttributes(Map<?,?> attributes);

    public abstract ContextAttributes withoutSharedAttribute(Object key);

    /*
    /**********************************************************
    /* Per-operation (serialize/deserialize) access
    /**********************************************************
     */

    /**
     * Accessor for value of specified attribute
     */
    public abstract Object getAttribute(Object key);

    /**
     * Mutator used during call (via context) to set value of "non-shared"
     * part of attribute set.
     */
    public abstract ContextAttributes withPerCallAttribute(Object key, Object value);

    /*
    /**********************************************************
    /* Default implementation
    /**********************************************************
     */

    public static class Impl extends ContextAttributes
        implements java.io.Serializable // just so ObjectReader/ObjectWriter can retain configs
    {
        private static final long serialVersionUID = 1L;

        protected final static Impl EMPTY = new Impl(Collections.emptyMap());

        protected final static Object NULL_SURROGATE = new Object();

        /**
         * Shared attributes that we cannot modify in-place.
         */
        protected final Map<?,?> _shared;

        /**
         * Per-call attributes that we can directly modify, since they are not
         * shared between threads.
         *<p>
         * NOTE: typed as Object-to-Object, unlike {@link #_shared}, because
         * we need to be able to modify contents, and wildcard type would
         * complicate that access.
         */
        protected transient Map<Object,Object> _nonShared;

        /*
        /**********************************************************
        /* Construction, factory methods
        /**********************************************************
         */

        protected Impl(Map<?,?> shared) {
            _shared = shared;
            _nonShared = null;
        }

        protected Impl(Map<?,?> shared, Map<Object,Object> nonShared) {
            _shared = shared;
            _nonShared = nonShared;
        }

        public static ContextAttributes getEmpty() {
            return EMPTY;
        }

        /*
        /**********************************************************
        /* Per-reader/writer mutant factories
        /**********************************************************
         */

        @Override
        public ContextAttributes withSharedAttribute(Object key, Object value)
        {
            Map<Object,Object> m;
            // need to cover one special case, since EMPTY uses Immutable map:
            if (this == EMPTY) {
                m = new HashMap<Object,Object>(8);
            } else {
                m = _copy(_shared);
            }
            m.put(key, value);
            return new Impl(m);
        }

        @Override
        public ContextAttributes withSharedAttributes(Map<?,?> shared) {
            return new Impl(shared);
        }

        @Override
        public ContextAttributes withoutSharedAttribute(Object key)
        {
            // first couple of trivial optimizations
            if (_shared.isEmpty()) {
                return this;
            }
            if (_shared.containsKey(key)) {
                if (_shared.size() == 1) {
                    return EMPTY;
                }
            } else { // if we didn't have it anyway, return as-is
                return this;
            }
            // otherwise make copy, modify
            Map<Object,Object> m = _copy(_shared);
            m.remove(key);
            return new Impl(m);
        }

        /*
        /**********************************************************
        /* Per-call access
        /**********************************************************
         */

        @Override
        public Object getAttribute(Object key)
        {
            if (_nonShared != null) {
                Object ob = _nonShared.get(key);
                if (ob != null) {
                    if (ob == NULL_SURROGATE) {
                        return null;
                    }
                    return ob;
                }
            }
            return _shared.get(key);
        }

        @Override
        public ContextAttributes withPerCallAttribute(Object key, Object value)
        {
            // First: null value may need masking
            if (value == null) {
                // need to mask nulls to ensure default values won't be showing
                if (_shared.containsKey(key)) {
                    value = NULL_SURROGATE;
                } else if ((_nonShared == null) || !_nonShared.containsKey(key)) {
                    // except if non-mutable shared list has no entry, we don't care
                    return this;
                } else {
                    _nonShared.remove(key);
                    return this;
                }
            }
            // a special case: create non-shared instance if need be
            if (_nonShared == null) {
                return nonSharedInstance(key, value);
            }
            _nonShared.put(key, value);
            return this;
        }

        /*
        /**********************************************************
        /* Internal methods
        /**********************************************************
         */

        /**
         * Overridable method that creates initial non-shared instance,
         * with the first explicit set value.
         */
        protected ContextAttributes nonSharedInstance(Object key, Object value)
        {
            Map<Object,Object> m = new HashMap<Object,Object>();
            if (value == null) {
                value = NULL_SURROGATE;
            }
            m.put(key, value);
            return new Impl(_shared, m);
        }

        private Map<Object,Object> _copy(Map<?,?> src)
        {
            return new HashMap<Object,Object>(src);
        }
    }
}
