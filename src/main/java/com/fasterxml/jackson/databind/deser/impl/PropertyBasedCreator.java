package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;

/**
 * Object that is used to collect arguments for non-default creator
 * (non-default-constructor, or argument-taking factory method)
 * before creator can be called.
 * Since ordering of JSON properties is not guaranteed, this may
 * require buffering of values other than ones being passed to
 * creator.
 */
public final class PropertyBasedCreator
{
    /**
     * Number of properties: usually same as size of {@link #_propertyLookup},
     * but not necessarily, when we have unnamed injectable properties.
     */
    protected final int _propertyCount;

    /**
     * Helper object that knows how to actually construct the instance by
     * invoking creator method with buffered arguments.
     */
    protected final ValueInstantiator _valueInstantiator;

    /**
     * Map that contains property objects for either constructor or factory
     * method (whichever one is null: one property for each
     * parameter for that one), keyed by logical property name
     */
    protected final HashMap<String, SettableBeanProperty> _propertyLookup;

    /**
     * Array that contains properties that expect value to inject, if any;
     * null if no injectable values are expected.
     */
    protected final SettableBeanProperty[] _allProperties;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    protected PropertyBasedCreator(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator,
            SettableBeanProperty[] creatorProps,
            boolean caseInsensitive,
            boolean addAliases)
    {
        _valueInstantiator = valueInstantiator;
        if (caseInsensitive) {
            _propertyLookup = CaseInsensitiveMap.construct(ctxt.getConfig().getLocale());
        } else {
            _propertyLookup = new HashMap<String, SettableBeanProperty>();
        }
        final int len = creatorProps.length;
        _propertyCount = len;
        _allProperties = new SettableBeanProperty[len];

        // 26-Feb-2017, tatu: Let's start by aliases, so that there is no
        //    possibility of accidental override of primary names
        if (addAliases) {
            final DeserializationConfig config = ctxt.getConfig();
            for (SettableBeanProperty prop : creatorProps) {
                // 22-Jan-2018, tatu: ignorable entries should be ignored, even if got aliases
                if (!prop.isIgnorable()) {
                    List<PropertyName> aliases = prop.findAliases(config);
                    if (!aliases.isEmpty()) {
                        for (PropertyName pn : aliases) {
                            _propertyLookup.put(pn.getSimpleName(), prop);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = creatorProps[i];
            _allProperties[i] = prop;
            // 22-Jan-2018, tatu: ignorable entries should be skipped
            if (!prop.isIgnorable()) {
                _propertyLookup.put(prop.getName(), prop);
            }
        }
    }

    /**
     * Factory method used for building actual instances to be used with POJOS:
     * resolves deserializers, checks for "null values".
     *
     * @since 2.9
     */
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps,
            BeanPropertyMap allProperties)
        throws JsonMappingException
    {
        final int len = srcCreatorProps.length;
        SettableBeanProperty[] creatorProps = new SettableBeanProperty[len];
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = srcCreatorProps[i];
            if (!prop.hasValueDeserializer()) {
                // 15-Apr-2020, tatu: [databind#962] Avoid getting deserializer for Inject-only
                //     cases
                if (!prop.isInjectionOnly()) {
                    prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
                }
            }
            creatorProps[i] = prop;
        }
        return new PropertyBasedCreator(ctxt, valueInstantiator, creatorProps,
                allProperties.isCaseInsensitive(),
// 05-Sep-2019, tatu: As per [databind#2378] looks like not all aliases get merged into
//    `allProperties` so force lookup anyway.
//                allProperties.hasAliases()
                true);
    }

    /**
     * Factory method used for building actual instances to be used with types
     * OTHER than POJOs.
     * resolves deserializers and checks for "null values".
     *
     * @since 2.9
     */
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps,
            boolean caseInsensitive)
        throws JsonMappingException
    {
        final int len = srcCreatorProps.length;
        SettableBeanProperty[] creatorProps = new SettableBeanProperty[len];
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = srcCreatorProps[i];
            if (!prop.hasValueDeserializer()) {
                prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
            }
            creatorProps[i] = prop;
        }
        return new PropertyBasedCreator(ctxt, valueInstantiator, creatorProps,
                caseInsensitive, false);
    }

    @Deprecated // since 2.9
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps)
        throws JsonMappingException
    {
        return construct(ctxt, valueInstantiator, srcCreatorProps,
                ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public Collection<SettableBeanProperty> properties() {
        return _propertyLookup.values();
    }

    public SettableBeanProperty findCreatorProperty(String name) {
        return _propertyLookup.get(name);
    }

    public SettableBeanProperty findCreatorProperty(int propertyIndex) {
        for (SettableBeanProperty prop : _propertyLookup.values()) {
            if (prop.getPropertyIndex() == propertyIndex) {
                return prop;
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Building process
    /**********************************************************
     */

    /**
     * Method called when starting to build a bean instance.
     *
     * @since 2.1 (added ObjectIdReader parameter -- existed in previous versions without)
     */
    public PropertyValueBuffer startBuilding(JsonParser p, DeserializationContext ctxt,
            ObjectIdReader oir) {
        return new PropertyValueBuffer(p, ctxt, _propertyCount, oir);
    }

    public Object build(DeserializationContext ctxt, PropertyValueBuffer buffer) throws IOException
    {
        Object bean = _valueInstantiator.createFromObjectWith(ctxt,
                _allProperties, buffer);
        // returning null isn't quite legal, but let's let caller deal with that
        if (bean != null) {
            // Object Id to handle?
            bean = buffer.handleIdValue(ctxt, bean);

            // Anything buffered?
            for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
                pv.assign(bean);
            }
        }
        return bean;
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Simple override of standard {@link java.util.HashMap} to support
     * case-insensitive access to creator properties
     *
     * @since 2.8.5
     */
    static class CaseInsensitiveMap extends HashMap<String, SettableBeanProperty>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Lower-casing can have Locale-specific minor variations.
         *
         * @since 2.11
         */
        protected final Locale _locale;

        @Deprecated // since 2.11
        public CaseInsensitiveMap() {
            this(Locale.getDefault());
        }

        // @since 2.11
        public CaseInsensitiveMap(Locale l) {
            _locale = l;
        }

        // @since 2.11
        public static CaseInsensitiveMap construct(Locale l) {
            return new CaseInsensitiveMap(l);
        }

        @Override
        public SettableBeanProperty get(Object key0) {
            return super.get(((String) key0).toLowerCase(_locale));
        }

        @Override
        public SettableBeanProperty put(String key, SettableBeanProperty value) {
            key = key.toLowerCase(_locale);
            return super.put(key, value);
        }
    }
}
