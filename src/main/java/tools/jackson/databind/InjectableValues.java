package tools.jackson.databind;

import java.util.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.util.ClassUtil;

/**
 * Abstract class that defines API for objects that provide value to
 * "inject" during deserialization. An instance of this object
 */
public abstract class InjectableValues
    implements Snapshottable<InjectableValues>
{
    public static InjectableValues empty() {
        return InjectableValues.Empty.INSTANCE;
    }

    /**
     * Method called to find value identified by id <code>valueId</code> to
     * inject as value of specified property during deserialization, passing
     * POJO instance in which value will be injected if it is available
     * (will be available when injected via field or setter; not available
     * when injected via constructor or factory method argument).
     *
     * @param ctxt Deserialization context
     * @param valueId Object that identifies value to inject; may be a simple
     *   name or more complex identifier object, whatever provider needs
     * @param forProperty Bean property in which value is to be injected
     * @param beanInstance Bean instance that contains property to inject,
     *    if available; null if bean has not yet been constructed.
     * @param optional Flag used for configuring the behavior when the value
     *    to inject is not found
     * @param useInput
     */
    public abstract Object findInjectableValue(DeserializationContext ctxt,
            Object valueId, BeanProperty forProperty, Object beanInstance,
            Boolean optional, Boolean useInput)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Standard implementations
    /**********************************************************************
     */

    /**
     * Shared intermediate base class for standard implementations.
     */
    public abstract static class Base
        extends InjectableValues
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected String _validateKey(DeserializationContext ctxt, Object valueId,
                BeanProperty forProperty, Object beanInstance)
            throws JacksonException
        {
            if (!(valueId instanceof String)) {
                throw ctxt.missingInjectableValueException(
                        String.format(
                        "Unsupported injectable value id type (%s), expecting String",
                        ClassUtil.classNameOf(valueId)),
                        valueId, forProperty, beanInstance);
            }
            return (String) valueId;
        }

        protected Object _handleMissingValue(DeserializationContext ctxt, String key,
                BeanProperty forProperty, Object beanInstance,
                Boolean optionalConfig, Boolean useInputConfig)
            throws JacksonException
        {
            // Different defaulting fo "optional" (default to FALSE) and
            // "useInput" (default to TRUE)

            final boolean optional = Boolean.TRUE.equals(optionalConfig);
            final boolean useInput = Boolean.TRUE.equals(useInputConfig);

            // [databind#1381]: 14-Nov-2025, tatu: This is a mess: (1) and (2) make sense
            //   but (3) is debatable. However, for backward compatibility this is what
            //   passes tests we have.

            // Missing ok if:
            //
            // 1. `optional` is TRUE
            // 2. FAIL_ON_UNKNOWN_INJECT_VALUE is disabled
            // 3. `useInput` is TRUE and injection is NOT via constructor (implied
            //    by beanInstance being non-null)
            if (optional
                    || !ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_INJECT_VALUE)
                    || (useInput && beanInstance != null)
                    ) {
                return null;
            }
            throw ctxt.missingInjectableValueException(
                    String.format("No injectable value with id '%s' found (for property '%s')",
                    key, forProperty.getName()),
                    key, forProperty, beanInstance);
        }
    }

    private static final class Empty
        extends Base
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        final static Empty INSTANCE = new Empty();

        @Override
        public Empty snapshot() {
            return this;
        }

        @Override
        public Object findInjectableValue(DeserializationContext ctxt, Object valueId,
                BeanProperty forProperty, Object beanInstance,
                Boolean optional, Boolean useInput)
            throws JacksonException
        {
            final String key = _validateKey(ctxt, valueId, forProperty, beanInstance);
            return _handleMissingValue(ctxt, key, forProperty, beanInstance,
                    optional, useInput);
        }
    }

    /**
     * Simple standard implementation which uses a simple Map to
     * store values to inject, identified by simple String keys.
     */
    public static class Std
        extends Base
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final Map<String,Object> _values;

        public Std() {
            this(new HashMap<>());
        }

        public Std(Map<String,Object> values) {
            _values = values;
        }

        public Std addValue(String key, Object value) {
            _values.put(key, value);
            return this;
        }

        public Std addValue(Class<?> classKey, Object value) {
            _values.put(classKey.getName(), value);
            return this;
        }

        @Override
        public Std snapshot() {
            if (_values.isEmpty()) {
                return new Std();
            }
            return new Std(new HashMap<>(_values));
        }

        @Override
        public Object findInjectableValue(DeserializationContext ctxt,
                Object valueId,
                BeanProperty forProperty, Object beanInstance,
                Boolean optional, Boolean useInput)
        {
            String key = _validateKey(ctxt, valueId, forProperty, beanInstance);
            Object ob = _values.get(key);
            if (ob == null && !_values.containsKey(key)) {
                return _handleMissingValue(ctxt, key, forProperty, beanInstance,
                        optional, useInput);
            }
            return ob;
        }
    }
}
