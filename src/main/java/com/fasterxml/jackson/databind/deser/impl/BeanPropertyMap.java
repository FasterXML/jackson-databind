package com.fasterxml.jackson.databind.deser.impl;

import java.util.*;

import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.util.Named;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
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

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected final boolean _caseInsensitive;

    /**
     * Configuration of alias mappings, if any (`null` if none),
     * aligned with properties in <code>_propsInOrder</code>
     */
    private final PropertyName[][] _aliasDefs;

    /**
     * Mapping from secondary names (aliases) to primary names.
     */
    private final Map<String,String> _aliasMapping;

    /*
    /**********************************************************
    /* Lookup information
    /**********************************************************
     */

    /**
     * Array of properties in the exact order they were handed in. This is
     * used by as-array serialization, deserialization.
     * Contains both primary properties (first <code>_primaryCount</code>
     * entries) and possible aliased mappings
     */
    private SettableBeanProperty[] _propsInOrder;

    /**
     * Lazily instantiated array of properties mapped from lookup index, in which
     * first entries are ame as in <code>_propsInOrder</code> followed by alias
     * mappings.
     */
    private SettableBeanProperty[] _propsWithAliases;

    /*
    /**********************************************************
    /* Local hash area, settings
    /**********************************************************
     */

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

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    /**
     * @param caseInsensitive Whether property name matching should case-insensitive or not
     * @param props Sequence of primary properties to index
     * @param aliasDefs Alias mappings, if any (null if none)
     * @param assignIndexes Whether to assign indices to property entities or not
     */
    protected BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
            PropertyName[][] aliasDefs,
            boolean assignIndexes)
    {
        _caseInsensitive = caseInsensitive;
        _aliasDefs = aliasDefs;
        _propsInOrder = props.toArray(new SettableBeanProperty[props.size()]);

        if (aliasDefs == null) {
            _aliasMapping = Collections.emptyMap();
        } else {
            _aliasMapping = _buildAliasMapping(props, aliasDefs);
        }
//        init(props);

        // Former `assignIndexes`
        // order is arbitrary, but stable:
        if (assignIndexes) {
            // note: only assign to primary entries, not to aliased (since they are dups)
            for (int i = 0, end = props.size(); i < end; ++i) {
                _propsInOrder[i].assignIndex(i);
            }
        }
    }

    protected BeanPropertyMap(BeanPropertyMap base, boolean caseInsensitive)
    {
        _caseInsensitive = caseInsensitive;
        _aliasDefs = base._aliasDefs;
        _aliasMapping = base._aliasMapping;

        // 16-May-2016, tatu: Alas, not enough to just change flag, need to re-init as well.
        _propsInOrder = Arrays.copyOf(base._propsInOrder, base._propsInOrder.length);
//        init(Arrays.asList(_propsInOrder));
    }

    private Map<String,String> _buildAliasMapping(Collection<SettableBeanProperty> props,
            PropertyName[][] aliasDefs)
    {
        // Ok, first, we need an actual index, so traverse over primary properties first
        Map<String,String> mapping = new HashMap<>();
        for (int i = 0, end = aliasDefs.length; i < end; ++i) {
            PropertyName[] aliases = aliasDefs[i];
            if (aliases != null) {
                SettableBeanProperty prop = _propsInOrder[i];
                String propId = prop.getName();
                if (_caseInsensitive) {
                    propId = propId.toLowerCase();
                }
                for (PropertyName alias : aliases) {
                    String aliasId = alias.getSimpleName();
                    if (_caseInsensitive) {
                        aliasId = aliasId.toLowerCase();
                    }
                    mapping.put(alias.getName(), propId);
                }
            }
        }
        return mapping;
    }

    public static BeanPropertyMap construct(Collection<SettableBeanProperty> props,
            boolean caseInsensitive, PropertyName[][] aliases)
    {
        return new BeanPropertyMap(caseInsensitive, props, aliases, true);
    }

    public void init() {
        init(Arrays.asList(_propsInOrder));
    }

    protected void init(Collection<SettableBeanProperty> props)
    {
        _size = props.size();

        // First: calculate size of primary hash area
        final int hashSize = findSize(_size);
        _hashMask = hashSize-1;

        // and allocate enough to contain primary/secondary, expand for spill-overs as need be
        int alloc = (hashSize + (hashSize>>1)) * 2;
        Object[] hashed = new Object[alloc];
        int spillCount = 0;

        for (SettableBeanProperty prop : props) {
            String key = _propertyName(prop);
            int slot = key.hashCode() & _hashMask;
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

    /*
    /**********************************************************
    /* "Mutant factory" methods
    /**********************************************************
     */

    /**
     * Mutant factory method that constructs a new instance if desired case-insensitivity
     * state differs from the state of this instance; if states are the same, returns
     * <code>this</code>.
     */
    public BeanPropertyMap withCaseInsensitivity(boolean state) {
        if (_caseInsensitive == state) {
            return this;
        }
        return new BeanPropertyMap(this, state);
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
        // First: maybe just replace in place?
        final String key = newProp.getName();
        for (int i = 0, end = _propsInOrder.length; i < end; ++i) {
            if (_propsInOrder[i].getName().equals(key)) {
                _propsInOrder[i] = newProp;
                return this;
            }
        }

        // If not, append
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(Arrays.asList(_propsInOrder));
        newProps.add(newProp);
        // !!! TODO: assign index for the last entry?
        return new BeanPropertyMap(_caseInsensitive, newProps, _aliasDefs, false);
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
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(_propsInOrder.length);
        for (int i = 0; i < len; ++i) {
            newProps.add(_rename(_propsInOrder[i], transformer));
        }
        // 26-Feb-2017, tatu: Probably SHOULD handle renaming wrt Aliases?
        // NOTE: do NOT try reassigning indexes of properties; number doesn't change

        // !!! 18-Nov-2017, tatu: For some reason we DO have to force init here -- should investigate why,
        //    try to remove need
        BeanPropertyMap map = new BeanPropertyMap(_caseInsensitive, newProps, _aliasDefs, false);
        map.init(newProps);
        return map;
    }

    private SettableBeanProperty _rename(SettableBeanProperty prop, NameTransformer xf)
    {
        if (prop != null) {
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
        }
        return prop;
    }
    
    /**
     * Mutant factory method that will use this instance as the base, and
     * construct an instance that is otherwise same except for excluding
     * properties with specified names.
     */
    public BeanPropertyMap withoutProperties(Collection<String> toExclude)
    {
        if (toExclude.isEmpty()) {
            return this;
        }
        final int len = _propsInOrder.length;
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(len);

        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _propsInOrder[i];
            if (!toExclude.contains(prop.getName())) {
                newProps.add(prop);
            }
        }
        // should we try to re-index? Apparently no need
        // 17-Nov-2017, tatu: do NOT try to change indexes since this could lead to discrepancies
        //    (unless we actually copy property instances)
        return new BeanPropertyMap(_caseInsensitive, newProps, _aliasDefs, false);
    }

    /**
     * Specialized method that can be used to replace an existing entry
     * (note: entry MUST exist; otherwise exception is thrown) with
     * specified replacement.
     */
    public void replace(SettableBeanProperty oldProp, SettableBeanProperty newProp)
    {
        for (int i = 0, end = _propsInOrder.length; i < end; ++i) {
            if (_propsInOrder[i] == oldProp) {
                _propsInOrder[i] = newProp;
                return;
            }
        }
        throw new NoSuchElementException("No entry '"+oldProp.getName()+"' found, can't replace");
    }

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public void remove(SettableBeanProperty propToRm)
    {
        final String key = _propertyName(propToRm);
        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_size);
        boolean found = false;
        for (SettableBeanProperty prop : _propsInOrder) {
            if (!found) {
                // Important: make sure to lower-case name to match as necessary
                String match = _propertyName(prop);
                if (found = match.equals(key)) {
                    continue;
                }
            }
            props.add(prop);
        }
        if (!found) {
            throw new NoSuchElementException("No entry '"+propToRm.getName()+"' found, can't remove");
        }
        _propsInOrder = props.toArray(new SettableBeanProperty[props.size()]);
    }

    /*
    /**********************************************************
    /* Factory method(s) for helpers
    /**********************************************************
     */

    public FieldNameMatcher constructMatcher(TokenStreamFactory tsf)
    {
        List<Named> names;
        if (_aliasDefs == null) { // simple case, no aliases
            _propsWithAliases = _propsInOrder;
            names = Arrays.asList(_propsInOrder);
        } else {
            // must make an actual copy (not just array-backed) as we'll append entries:
            List<SettableBeanProperty> allProps = new ArrayList<>(Arrays.asList(_propsInOrder));
            names = new ArrayList<>(allProps);

            // map aliases
            for (int i = 0, end = _aliasDefs.length; i < end; ++i) {
                PropertyName[] aliases = _aliasDefs[i];
                if (aliases != null) {
                    SettableBeanProperty primary = _propsInOrder[i];
                    for (PropertyName alias : aliases) {
                        names.add(alias);
                        allProps.add(primary);
                    }
                }
            }
            _propsWithAliases = allProps.toArray(new SettableBeanProperty[allProps.size()]);
        }
        // `true` -> yes, they are intern()ed alright
        if (_caseInsensitive) {
            return tsf.constructCIFieldNameMatcher(names, true);
        }
        return tsf.constructFieldNameMatcher(names, true);
    }

    /**
     * Method similar to {@link #getPrimaryProperties()} but will append aliased
     * properties after primary ones
     */
    public SettableBeanProperty[] getPropertiesWithAliases() {
        return _propsWithAliases;
    }

    /*
    /**********************************************************
    /* Public API, simple accessors
    /**********************************************************
     */

    public int size() { return _size; }

    public boolean isCaseInsensitive() {
        return _caseInsensitive;
    }

    public boolean hasAliases() {
        return _aliasDefs != null;
    }

    /**
     * Accessor for traversing over all contained properties.
     */
    @Override
    public Iterator<SettableBeanProperty> iterator() {
        return Arrays.asList(_propsInOrder).iterator();
    }

    /**
     * Method that will re-create initial insertion-ordering of
     * properties contained in this map. Note that if properties
     * have been removed, array may contain nulls; otherwise
     * it should be consecutive.
     */
    public SettableBeanProperty[] getPrimaryProperties() {
        return _propsInOrder;
    }

    /*
    /**********************************************************
    /* Public API, property definition lookup
    /**********************************************************
     */

    public SettableBeanProperty findDefinition(int index)
    {
        for (int i = 0 ,end = _propsInOrder.length; i < end; ++i) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if ((prop != null) && (index == prop.getPropertyIndex())) {
                return prop;
            }
        }
        return null;
    }

    /**
     * NOTE: does NOT do case-insensitive matching -- only to be used during construction
     * and never during deserialization process -- nor alias expansion.
     */
    public SettableBeanProperty findPrimaryDefinition(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Cannot pass null property name");
        }
        for (SettableBeanProperty prop : _propsInOrder) {
            if (key.equals(prop.getName())) {
                return prop;
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Public API, property lookup
    /**********************************************************
     */

    public SettableBeanProperty find(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Cannot pass null property name");
        }
        if (_caseInsensitive) {
            key = key.toLowerCase();
        }

        // inlined `_hashCode(key)`
        int slot = key.hashCode() & _hashMask;

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
            return _findWithAlias(_aliasMapping.get(key));
        }
        // no? secondary?
        int hashSize = _hashMask+1;
        int ix = hashSize + (slot>>1) << 1;
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
        return _findWithAlias(_aliasMapping.get(key));
    }

    private SettableBeanProperty _findWithAlias(String keyFromAlias)
    {
        if (keyFromAlias == null) {
            return null;
        }
        // NOTE: need to inline much of handling do avoid cyclic calls via alias
        // first, inlined main `find(String)`
        int slot = keyFromAlias.hashCode() & _hashMask;
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
        int ix = hashSize + (slot>>1) << 1;
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

    // Confining this case insensitivity to this function (and the find method) in case we want to
    // apply a particular locale to the lower case function.  For now, using the default.
    protected final String _propertyName(SettableBeanProperty prop) {
        if (_caseInsensitive) {
            return prop.getName().toLowerCase();
        }
        return prop.getName();
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
            sb.append(String.format("%s(%s)", prop.getName(), prop.getType()));
        }
        sb.append(']');
        if (_aliasDefs != null) {
            sb.append(String.format("(aliases: %s)", _aliasMapping));
        }
        return sb.toString();
    }
}
