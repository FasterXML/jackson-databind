package tools.jackson.databind.deser.bean;

import java.util.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.SettableAnyProperty;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.impl.ManagedReferenceProperty;
import tools.jackson.databind.deser.impl.ObjectIdReader;
import tools.jackson.databind.util.NameTransformer;

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
     * Array that contains properties that match creator properties
     */
    protected final SettableBeanProperty[] _propertiesInOrder;

    /**
     * Indexes of properties with associated Injectable values, if any:
     * {@code null} if none.
     *
     * @since 2.21
     */
    protected final BitSet _injectablePropIndexes;

    /**
     * Flag indicating whether any creator properties are ManagedReferenceProperty instances,
     * used to optimize deserialization by skipping back-reference injection when not needed.
     *
     * @since 3.1
     */
    protected final boolean _hasManagedReferenceProperties;

    /*
    /**********************************************************************
    /* Construction, initialization
    /**********************************************************************
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
            _propertyLookup = new HashMap<>();
        }

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
        final int len = creatorProps.length;
        _propertyCount = len;
        _propertiesInOrder = new SettableBeanProperty[len];
        BitSet injectablePropIndexes = null;
        boolean hasManagedRef = false;

        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = creatorProps[i];
            _propertiesInOrder[i] = prop;
            // 22-Jan-2018, tatu: ignorable entries should be skipped
            if (!prop.isIgnorable()) {
                _propertyLookup.put(prop.getName(), prop);
            }
            if (prop.getInjectionDefinition() != null) {
                if (injectablePropIndexes == null) {
                    injectablePropIndexes = new BitSet(len);
                }
                injectablePropIndexes.set(i);
            }

            // [databind#1516]: detect whether any ManagedReferenceProperty exists
            // so we can avoid iterating over all properties when none are present
            hasManagedRef |= prop instanceof ManagedReferenceProperty;
        }

        _injectablePropIndexes = injectablePropIndexes;
        _hasManagedReferenceProperties = hasManagedRef;
    }

    protected PropertyBasedCreator(PropertyBasedCreator base,
            HashMap<String, SettableBeanProperty> propertyLookup,
            SettableBeanProperty[] allProperties)
    {
        _propertyCount = base._propertyCount;
        _valueInstantiator = base._valueInstantiator;
        _injectablePropIndexes = base._injectablePropIndexes;
        _propertyLookup = propertyLookup;
        _propertiesInOrder = allProperties;
        _hasManagedReferenceProperties = base._hasManagedReferenceProperties;
    }

    /**
     * Factory method used for building actual instances to be used with POJOS:
     * resolves deserializers, checks for "null values".
     */
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps,
            BeanPropertyMap allProperties)
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
     */
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcCreatorProps,
            boolean caseInsensitive)
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

    /**
     * Mutant factory method for constructing a map where the names of all properties
     * are transformed using the given {@link NameTransformer}.
     *
     * @since 2.19
     */
    public PropertyBasedCreator renameAll(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        if (transformer == null || (transformer == NameTransformer.NOP)) {
            return this;
        }

        final int len = _propertiesInOrder.length;
        HashMap<String, SettableBeanProperty> newLookup = new HashMap<>(_propertyLookup);
        List<SettableBeanProperty> newProps = new ArrayList<>(len);

        for (SettableBeanProperty prop : _propertiesInOrder) {
            if (prop == null) {
                newProps.add(null);
                continue;
            }

            SettableBeanProperty renamedProperty = prop.unwrapped(ctxt, transformer);
            String oldName = prop.getName();
            String newName = renamedProperty.getName();

            newProps.add(renamedProperty);

            if (!oldName.equals(newName) && newLookup.containsKey(oldName)) {
                newLookup.remove(oldName);
                newLookup.put(newName, renamedProperty);
            }
        }

        return new PropertyBasedCreator(this,
                newLookup,
                newProps.toArray(new SettableBeanProperty[0])
        );
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
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

    /**
     * @since 3.1
     */
    public boolean hasManagedReferenceProperties() {
        return _hasManagedReferenceProperties;
    }

    /*
    /**********************************************************************
    /* Building process
    /**********************************************************************
     */

    /**
     * Method called when starting to build a bean instance.
     */
    public PropertyValueBuffer startBuilding(JsonParser p, DeserializationContext ctxt,
            ObjectIdReader oir) {
        return new PropertyValueBuffer(p, ctxt, _propertyCount, oir, null,
                _injectablePropIndexes);
    }

    /**
     * Method called when starting to build a bean instance.
     *
     * @since 2.18 (added SettableAnyProperty parameter)
     */
    public PropertyValueBuffer startBuildingWithAnySetter(JsonParser p, DeserializationContext ctxt,
            ObjectIdReader oir, SettableAnyProperty anySetter
    ) {
        return new PropertyValueBuffer(p, ctxt, _propertyCount, oir, anySetter,
                _injectablePropIndexes);
    }

    public Object build(DeserializationContext ctxt, PropertyValueBuffer buffer)
        throws JacksonException
    {
        Object bean = _valueInstantiator.createFromObjectWith(ctxt,
                _propertiesInOrder, buffer);
        // returning null isn't quite legal, but let's let caller deal with that
        if (bean != null) {
            // Object Id to handle?
            bean = buffer.handleIdValue(ctxt, bean);

            // Anything buffered?
            for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
                pv.assign(ctxt, bean);
            }
        }
        return bean;
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Simple override of standard {@link java.util.HashMap} to support
     * case-insensitive access to creator properties
     */
    static class CaseInsensitiveMap extends HashMap<String, SettableBeanProperty>
    {
        // doesn't really need to be Serializable with 3.x but... whatever
        private static final long serialVersionUID = 3L;

        /**
         * Lower-casing can have Locale-specific minor variations.
         */
        protected final Locale _locale;

        public CaseInsensitiveMap(Locale l) {
            _locale = l;
        }

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
