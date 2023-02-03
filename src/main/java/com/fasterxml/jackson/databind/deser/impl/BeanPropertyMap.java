package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Helper class used for storing mapping from property name to
 * {@link SettableBeanProperty} instances.
 *<p>
 * Note that this class is used instead of generic {@link java.util.HashMap}
 * for bit of performance gain (and some memory savings): although default
 * implementation is very good for generic use cases, it can be streamlined
 * a bit for specific use case we have. Even relatively small improvements
 * matter since this is directly on the critical path during deserialization,
 * as it is done for each and every POJO property deserialized.
 */
public class BeanPropertyMap
    implements Iterable<SettableBeanProperty>,
        java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     * @since 2.5
     */
    protected final boolean _caseInsensitive;

    private int _hashMask;

    /**
     * Number of entries stored in the hash area.
     */
    private int _size;

    private int _spillCount;

    /**
     * Hash area that contains key/property pairs in adjacent elements.
     */
    private Object[] _hashArea;

    /**
     * Array of properties in the exact order they were handed in. This is
     * used by as-array serialization, deserialization.
     */
    private final SettableBeanProperty[] _propsInOrder;

    /**
     * Configuration of alias mappings, indexed by unmodified property name
     * to unmodified aliases, if any; entries only included for properties
     * that do have aliases.
     * This is is used for constructing actual reverse lookup mapping, if
     * needed, taking into account possible case-insensitivity, as well
     * as possibility of name prefixes.
     *
     * @since 2.9
     */
    private final Map<String,List<PropertyName>> _aliasDefs;

    /**
     * Mapping from secondary names (aliases) to primary names.
     *
     * @since 2.9
     */
    private final Map<String,String> _aliasMapping;

    /**
     * We require {@link Locale} since case changes are locale-sensitive in certain
     * cases (see <a href="https://en.wikipedia.org/wiki/Dotted_and_dotless_I">Turkish I</a>
     * for example)
     *
     * @since 2.11
     */
    private final Locale _locale;

    /**
     * @since 2.11
     */
    public BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
            Map<String,List<PropertyName>> aliasDefs,
            Locale locale)
    {
        _caseInsensitive = caseInsensitive;
        _propsInOrder = props.toArray(new SettableBeanProperty[props.size()]);
        _aliasDefs = aliasDefs;
        _locale = locale;
        _aliasMapping = _buildAliasMapping(aliasDefs, caseInsensitive, locale);
        init(props);

    }

    /**
     * @deprecated since 2.11
     */
    @Deprecated
    public BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
            Map<String,List<PropertyName>> aliasDefs) {
        this(caseInsensitive, props, aliasDefs, Locale.getDefault());
    }

    /* Copy constructors used when a property can replace existing one
     *
     * @since 2.9.6
     */
    private BeanPropertyMap(BeanPropertyMap src,
            SettableBeanProperty newProp, int hashIndex, int orderedIndex)
    {
        // First, copy most fields as is:
        _caseInsensitive = src._caseInsensitive;
        _locale = src._locale;
        _hashMask = src._hashMask;
        _size = src._size;
        _spillCount = src._spillCount;
        _aliasDefs = src._aliasDefs;
        _aliasMapping = src._aliasMapping;

        // but then make deep copy of arrays to modify
        _hashArea = Arrays.copyOf(src._hashArea, src._hashArea.length);
        _propsInOrder = Arrays.copyOf(src._propsInOrder, src._propsInOrder.length);
        _hashArea[hashIndex] = newProp;
        _propsInOrder[orderedIndex] = newProp;
    }

    /* Copy constructors used when a property needs to be appended (can't replace)
     *
     * @since 2.9.6
     */
    private BeanPropertyMap(BeanPropertyMap src,
            SettableBeanProperty newProp, String key, int slot)
    {
        // First, copy most fields as is:
        _caseInsensitive = src._caseInsensitive;
        _locale = src._locale;
        _hashMask = src._hashMask;
        _size = src._size;
        _spillCount = src._spillCount;
        _aliasDefs = src._aliasDefs;
        _aliasMapping = src._aliasMapping;

        // but then make deep copy of arrays to modify
        _hashArea = Arrays.copyOf(src._hashArea, src._hashArea.length);
        int last = src._propsInOrder.length;
        // and append property at the end of ordering
        _propsInOrder = Arrays.copyOf(src._propsInOrder, last+1);
        _propsInOrder[last] = newProp;

        final int hashSize = _hashMask+1;
        int ix = (slot<<1);

        // primary slot not free?
        if (_hashArea[ix] != null) {
            // secondary?
            ix = (hashSize + (slot >> 1)) << 1;
            if (_hashArea[ix] != null) {
                // ok, spill over.
                ix = ((hashSize + (hashSize >> 1) ) << 1) + _spillCount;
                _spillCount += 2;
                if (ix >= _hashArea.length) {
                    _hashArea = Arrays.copyOf(_hashArea, _hashArea.length + 4);
                }
            }
        }
        _hashArea[ix] = key;
        _hashArea[ix+1] = newProp;
    }

    /**
     * @since 2.8
     */
    protected BeanPropertyMap(BeanPropertyMap base, boolean caseInsensitive)
    {
        _caseInsensitive = caseInsensitive;
        _locale = base._locale;
        _aliasDefs = base._aliasDefs;
        _aliasMapping = base._aliasMapping;

        // 16-May-2016, tatu: Alas, not enough to just change flag, need to re-init as well.
        _propsInOrder = Arrays.copyOf(base._propsInOrder, base._propsInOrder.length);
        init(Arrays.asList(_propsInOrder));
    }

    /**
     * Mutant factory method that constructs a new instance if desired case-insensitivity
     * state differs from the state of this instance; if states are the same, returns
     * <code>this</code>.
     *
     * @since 2.8
     */
    public BeanPropertyMap withCaseInsensitivity(boolean state) {
        if (_caseInsensitive == state) {
            return this;
        }
        return new BeanPropertyMap(this, state);
    }

    protected void init(Collection<SettableBeanProperty> props)
    {
        _size = props.size();

        // First: calculate size of primary hash area
        final int hashSize = findSize(_size);
        _hashMask = hashSize-1;

        // and allocate enough to contain primary/secondary, expand for spillovers as need be
        int alloc = (hashSize + (hashSize>>1)) * 2;
        Object[] hashed = new Object[alloc];
        int spillCount = 0;

        for (SettableBeanProperty prop : props) {
            // Due to removal, renaming, theoretically possible we'll have "holes" so:
            if (prop == null) {
                continue;
            }

            String key = getPropertyName(prop);
            int slot = _hashCode(key);
            int ix = (slot<<1);

            // primary slot not free?
            if (hashed[ix] != null) {
                // secondary?
                ix = (hashSize + (slot >> 1)) << 1;
                if (hashed[ix] != null) {
                    // ok, spill over.
                    ix = ((hashSize + (hashSize >> 1) ) << 1) + spillCount;
                    spillCount += 2;
                    if (ix >= hashed.length) {
                        hashed = Arrays.copyOf(hashed, hashed.length + 4);
                    }
                }
            }
            hashed[ix] = key;
            hashed[ix+1] = prop;

            // and aliases
        }
        _hashArea = hashed;
        _spillCount = spillCount;
    }

    private final static int findSize(int size)
    {
        if (size <= 5) {
            return 8;
        }
        if (size <= 12) {
            return 16;
        }
        int needed = size + (size >> 2); // at most 80% full
        int result = 32;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    /**
     * @since 2.12
     */
    public static BeanPropertyMap construct(MapperConfig<?> config,
            Collection<SettableBeanProperty> props,
            Map<String,List<PropertyName>> aliasMapping,
            boolean caseInsensitive) {
        return new BeanPropertyMap(caseInsensitive,
                props, aliasMapping,
                config.getLocale());
    }

    /**
     * @since 2.11
     * @deprecated since 2.12
     */
    @Deprecated
    public static BeanPropertyMap construct(MapperConfig<?> config,
            Collection<SettableBeanProperty> props,
            Map<String,List<PropertyName>> aliasMapping) {
        return new BeanPropertyMap(config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES),
                props, aliasMapping,
                config.getLocale());
    }

    /**
     * @deprecated since 2.11
     */
    @Deprecated
    public static BeanPropertyMap construct(Collection<SettableBeanProperty> props,
            boolean caseInsensitive, Map<String,List<PropertyName>> aliasMapping) {
        return new BeanPropertyMap(caseInsensitive, props, aliasMapping);
    }

    /**
     * Fluent copy method that creates a new instance that is a copy
     * of this instance except for one additional property that is
     * passed as the argument.
     * Note that method does not modify this instance but constructs
     * and returns a new one.
     */
    public BeanPropertyMap withProperty(SettableBeanProperty newProp)
    {
        // First: may be able to just replace?
        String key = getPropertyName(newProp);

        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if ((prop != null) && prop.getName().equals(key)) {
                return new BeanPropertyMap(this, newProp, i, _findFromOrdered(prop));
            }
        }
        // If not, append
        final int slot = _hashCode(key);

        return new BeanPropertyMap(this, newProp, key, slot);
    }

    public BeanPropertyMap assignIndexes()
    {
        // order is arbitrary, but stable:
        int index = 0;
        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if (prop != null) {
                prop.assignIndex(index++);
            }
        }
        return this;
    }

    /**
     * Mutant factory method for constructing a map where all entries use given
     * prefix
     */
    public BeanPropertyMap renameAll(NameTransformer transformer)
    {
        if (transformer == null || (transformer == NameTransformer.NOP)) {
            return this;
        }
        // Try to retain insertion ordering as well
        final int len = _propsInOrder.length;
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(len);

        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _propsInOrder[i];

            // What to do with holes? For now, retain
            if (prop == null) {
                newProps.add(prop);
                continue;
            }
            newProps.add(_rename(prop, transformer));
        }
        // should we try to re-index? Ordering probably changed but caller probably doesn't want changes...
        // 26-Feb-2017, tatu: Probably SHOULD handle renaming wrt Aliases?
        return new BeanPropertyMap(_caseInsensitive, newProps, _aliasDefs, _locale);
    }

    /*
    /**********************************************************
    /* Public API, mutators
    /**********************************************************
     */

    /**
     * Mutant factory method that will use this instance as the base, and
     * construct an instance that is otherwise same except for excluding
     * properties with specified names.
     *
     * @since 2.8
     */
    public BeanPropertyMap withoutProperties(Collection<String> toExclude)
    {
        return withoutProperties(toExclude, null);
    }

    /**
     * Mutant factory method that will use this instance as the base, and
     * construct an instance that is otherwise same except for excluding
     * properties with specified names, or including only the one marked
     * as included
     *
     * @since 2.12
     */
    public BeanPropertyMap withoutProperties(Collection<String> toExclude, Collection<String> toInclude)
    {
        if ((toExclude == null || toExclude.isEmpty()) && toInclude == null) {
            return this;
        }
        final int len = _propsInOrder.length;
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(len);

        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _propsInOrder[i];
            // 01-May-2015, tatu: Not 100% sure if existing `null`s should be retained;
            //   or, if entries to ignore should be retained as nulls. For now just
            //   prune them out
            if (prop != null) { // may contain holes, too, check.
                if (!IgnorePropertiesUtil.shouldIgnore(prop.getName(), toExclude, toInclude)) {
                    newProps.add(prop);
                }
            }
        }
        // should we try to re-index? Apparently no need
        return new BeanPropertyMap(_caseInsensitive, newProps, _aliasDefs, _locale);
    }

    /**
     * Specialized method that can be used to replace an existing entry
     * (note: entry MUST exist; otherwise exception is thrown) with
     * specified replacement.
     *
     * @since 2.9.4
     */
    public void replace(SettableBeanProperty origProp, SettableBeanProperty newProp)
    {
        int i = 1;
        int end = _hashArea.length;

        for (;; i += 2) {
            if (i >= end) {
                throw new NoSuchElementException("No entry '"+origProp.getName()+"' found, can't replace");
            }
            if (_hashArea[i] == origProp) {
                _hashArea[i] = newProp;
                break;
            }
        }
        _propsInOrder[_findFromOrdered(origProp)] = newProp;
    }

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public void remove(SettableBeanProperty propToRm)
    {
        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_size);
        String key = getPropertyName(propToRm);
        boolean found = false;

        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if (prop == null) {
                continue;
            }
            if (!found) {
                // 09-Jan-2017, tatu: Important: must check name slot and NOT property name,
                //   as only former is lower-case in case-insensitive case
                found = key.equals(_hashArea[i-1]);
                if (found) {
                    // need to leave a hole here
                    _propsInOrder[_findFromOrdered(prop)] = null;
                    continue;
                }
            }
            props.add(prop);
        }
        if (!found) {
            throw new NoSuchElementException("No entry '"+propToRm.getName()+"' found, can't remove");
        }
        init(props);
    }

    /*
    /**********************************************************
    /* Public API, simple accessors
    /**********************************************************
     */

    public int size() { return _size; }

    /**
     * @since 2.9
     */
    public boolean isCaseInsensitive() {
        return _caseInsensitive;
    }

    /**
     * @since 2.9
     */
    public boolean hasAliases() {
        return !_aliasDefs.isEmpty();
    }

    /**
     * Accessor for traversing over all contained properties.
     */
    @Override
    public Iterator<SettableBeanProperty> iterator() {
        return _properties().iterator();
    }

    private List<SettableBeanProperty> _properties() {
        ArrayList<SettableBeanProperty> p = new ArrayList<SettableBeanProperty>(_size);
        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if (prop != null) {
                p.add(prop);
            }
        }
        return p;
    }

    /**
     * Method that will re-create initial insertion-ordering of
     * properties contained in this map. Note that if properties
     * have been removed, array may contain nulls; otherwise
     * it should be consecutive.
     *
     * @since 2.1
     */
    public SettableBeanProperty[] getPropertiesInInsertionOrder() {
        return _propsInOrder;
    }

    // Confining this case insensitivity to this function (and the find method) in case we want to
    // apply a particular locale to the lower case function.  For now, using the default.
    protected final String getPropertyName(SettableBeanProperty prop) {
        return _caseInsensitive ? prop.getName().toLowerCase(_locale) : prop.getName();
    }

    /*
    /**********************************************************
    /* Public API, property lookup
    /**********************************************************
     */

    /**
     * @since 2.3
     */
    public SettableBeanProperty find(int index)
    {
        // note: will scan the whole area, including primary, secondary and
        // possible spill-area
        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if ((prop != null) && (index == prop.getPropertyIndex())) {
                return prop;
            }
        }
        return null;
    }

    public SettableBeanProperty find(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Cannot pass null property name");
        }
        if (_caseInsensitive) {
            key = key.toLowerCase(_locale);
        }

        // inlined `_hashCode(key)`
        int slot = key.hashCode() & _hashMask;
//        int h = key.hashCode();
//        int slot = (h + (h >> 13)) & _hashMask;

        int ix = (slot<<1);
        Object match = _hashArea[ix];
        if ((match == key) || key.equals(match)) {
            return (SettableBeanProperty) _hashArea[ix+1];
        }
        return _find2(key, slot, match);
    }

    private final SettableBeanProperty _find2(String key, int slot, Object match)
    {
        if (match == null) {
            // 26-Feb-2017, tatu: Need to consider aliases
            return _findWithAlias(_aliasMapping.get(key));
        }
        // no? secondary?
        int hashSize = _hashMask+1;
        int ix = (hashSize + (slot>>1)) << 1;
        match = _hashArea[ix];
        if (key.equals(match)) {
            return (SettableBeanProperty) _hashArea[ix+1];
        }
        if (match != null) { // _findFromSpill(...)
            int i = (hashSize + (hashSize>>1)) << 1;
            for (int end = i + _spillCount; i < end; i += 2) {
                match = _hashArea[i];
                if ((match == key) || key.equals(match)) {
                    return (SettableBeanProperty) _hashArea[i+1];
                }
            }
        }
        // 26-Feb-2017, tatu: Need to consider aliases
        return _findWithAlias(_aliasMapping.get(key));
    }

    private SettableBeanProperty _findWithAlias(String keyFromAlias)
    {
        if (keyFromAlias == null) {
            return null;
        }
        // NOTE: need to inline much of handling do avoid cyclic calls via alias
        // first, inlined main `find(String)`
        int slot = _hashCode(keyFromAlias);
        int ix = (slot<<1);
        Object match = _hashArea[ix];
        if (keyFromAlias.equals(match)) {
            return (SettableBeanProperty) _hashArea[ix+1];
        }
        if (match == null) {
            return null;
        }
        return _find2ViaAlias(keyFromAlias, slot, match);
    }

    private SettableBeanProperty _find2ViaAlias(String key, int slot, Object match)
    {
        // no? secondary?
        int hashSize = _hashMask+1;
        int ix = (hashSize + (slot>>1)) << 1;
        match = _hashArea[ix];
        if (key.equals(match)) {
            return (SettableBeanProperty) _hashArea[ix+1];
        }
        if (match != null) { // _findFromSpill(...)
            int i = (hashSize + (hashSize>>1)) << 1;
            for (int end = i + _spillCount; i < end; i += 2) {
                match = _hashArea[i];
                if ((match == key) || key.equals(match)) {
                    return (SettableBeanProperty) _hashArea[i+1];
                }
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Public API, deserialization support
    /**********************************************************
     */

    /**
     * Convenience method that tries to find property with given name, and
     * if it is found, call {@link SettableBeanProperty#deserializeAndSet}
     * on it, and return true; or, if not found, return false.
     * Note, too, that if deserialization is attempted, possible exceptions
     * are wrapped if and as necessary, so caller need not handle those.
     *
     * @since 2.5
     */
    public boolean findDeserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object bean, String key) throws IOException
    {
        final SettableBeanProperty prop = find(key);
        if (prop == null) {
            return false;
        }
        try {
            prop.deserializeAndSet(p, ctxt, bean);
        } catch (Exception e) {
            wrapAndThrow(e, bean, key, ctxt);
        }
        return true;
    }

    /*
    /**********************************************************
    /* Std method overrides
    /**********************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Properties=[");
        int count = 0;

        Iterator<SettableBeanProperty> it = iterator();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            if (count++ > 0) {
                sb.append(", ");
            }
            sb.append(prop.getName());
            sb.append('(');
            sb.append(prop.getType());
            sb.append(')');
        }
        sb.append(']');
        if (!_aliasDefs.isEmpty()) {
            sb.append("(aliases: ");
            sb.append(_aliasDefs);
            sb.append(")");
        }
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected SettableBeanProperty _rename(SettableBeanProperty prop, NameTransformer xf)
    {
        if (prop == null) {
            return prop;
        }
        String newName = xf.transform(prop.getName());
        prop = prop.withSimpleName(newName);
        JsonDeserializer<?> deser = prop.getValueDeserializer();
        if (deser != null) {
            @SuppressWarnings("unchecked")
            JsonDeserializer<Object> newDeser = (JsonDeserializer<Object>)
                deser.unwrappingDeserializer(xf);
            if (newDeser != deser) {
                prop = prop.withValueDeserializer(newDeser);
            }
        }
        return prop;
    }

    protected void wrapAndThrow(Throwable t, Object bean, String fieldName, DeserializationContext ctxt)
        throws IOException
    {
        // inlined 'throwOrReturnThrowable'
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors to be passed as is
        ClassUtil.throwIfError(t);
        // StackOverflowErrors are tricky ones; need to be careful...
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap JSON exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JacksonException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // allow disabling wrapping for unchecked exceptions
            ClassUtil.throwIfRTE(t);
        }
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    /**
     * Helper method used to find exact location of a property with name
     * given exactly, not subject to case changes, within hash area.
     * Expectation is that such property SHOULD exist, although no
     * exception is thrown.
     *
     * @since 2.7
     */
    /*
    private final int _findIndexInHash(String key)
    {
        final int slot = _hashCode(key);
        int ix = (slot<<1);

        // primary match?
        if (key.equals(_hashArea[ix])) {
            return ix+1;
        }
        // no? secondary?
        int hashSize = _hashMask+1;
        ix = hashSize + (slot>>1) << 1;
        if (key.equals(_hashArea[ix])) {
            return ix+1;
        }
        // perhaps spill then
        int i = (hashSize + (hashSize>>1)) << 1;
        for (int end = i + _spillCount; i < end; i += 2) {
            if (key.equals(_hashArea[i])) {
                return i+1;
            }
        }
        return -1;
    }
    */

    private final int _findFromOrdered(SettableBeanProperty prop) {
        for (int i = 0, end = _propsInOrder.length; i < end; ++i) {
            if (_propsInOrder[i] == prop) {
                return i;
            }
        }
        throw new IllegalStateException("Illegal state: property '"+prop.getName()+"' missing from _propsInOrder");
    }

    // Offlined version for convenience if we want to change hashing scheme
    private final int _hashCode(String key) {
        // This method produces better hash, fewer collisions... yet for some
        // reason produces slightly worse performance. Very strange.

        // 05-Aug-2015, tatu: ... still true?

        /*
        int h = key.hashCode();
        return (h + (h >> 13)) & _hashMask;
        */
        return key.hashCode() & _hashMask;
    }

    // @since 2.9
    private Map<String,String> _buildAliasMapping(Map<String,List<PropertyName>> defs,
            boolean caseInsensitive, Locale loc)
    {
        if ((defs == null) || defs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,String> aliases = new HashMap<>();
        for (Map.Entry<String,List<PropertyName>> entry : defs.entrySet()) {
            String key = entry.getKey();
            if (caseInsensitive) {
                key = key.toLowerCase(loc);
            }
            for (PropertyName pn : entry.getValue()) {
                String mapped = pn.getSimpleName();
                if (caseInsensitive) {
                    mapped = mapped.toLowerCase(loc);
                }
                aliases.put(mapped, key);
            }
        }
        return aliases;
    }
}
