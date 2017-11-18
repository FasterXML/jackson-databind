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
     * Configuration of alias mappings, indexed by unmodified property name
     * to unmodified aliases, if any; entries only included for properties
     * that do have aliases.
     * This is is used for constructing actual reverse lookup mapping, if
     * needed, taking into account possible case-insensitivity, as well
     * as possibility of name prefixes.
     */
    private final Map<String,List<PropertyName>> _aliasDefs;

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
     * Number of primary properties within <code>_propsInOrder</code>
     */
    private int _primaryCount;

    /**
     * Array of properties in the exact order they were handed in. This is
     * used by as-array serialization, deserialization.
     * Contains both primary properties (first <code>_primaryCount</code>
     * entries) and possible aliased mappings
     */
    private SettableBeanProperty[] _propsInOrder;

    /**
     * Array of names mapping to properties in <code>_propsInOrder</code>
     */
    private Named[] _namesInOrder;

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
     * @param aliasDefs Mapping of optional aliases
     * @param assignIndexes Whether to index properties or not
     */
    protected BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
            Map<String,List<PropertyName>> aliasDefs,
            boolean assignIndexes)
    {
        _caseInsensitive = caseInsensitive;
        _primaryCount = props.size();

//        int aliasCount = aliasaD
        _propsInOrder = props.toArray(new SettableBeanProperty[_primaryCount]);
        
        _aliasDefs = aliasDefs;
        _aliasMapping = _buildAliasMapping(aliasDefs);
//        init(props);

        // Former `assignIndexes`
        // order is arbitrary, but stable:
        if (assignIndexes) {
            int index = 0;
    
            for (SettableBeanProperty prop : _propsInOrder) {
                prop.assignIndex(index++);
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
        _primaryCount = base._primaryCount;
//        init(Arrays.asList(_propsInOrder));
    }

    public static BeanPropertyMap construct(Collection<SettableBeanProperty> props,
            boolean caseInsensitive, Map<String,List<PropertyName>> aliasMapping)
    {
        return new BeanPropertyMap(caseInsensitive, props, aliasMapping, true);
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
            String key = getPropertyName(prop);
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
        // First: may be able to just replace?
        /*
        String key = getPropertyName(newProp);

        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if ((prop != null) && prop.getName().equals(key)) {
                _hashArea[i] = newProp;
                _propsInOrder[_findFromOrdered(prop)] = newProp;
                return this;
            }
        }
        // If not, append
        final int slot = key.hashCode() & _hashMask;
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

        int last = _propsInOrder.length;
        _propsInOrder = Arrays.copyOf(_propsInOrder, last+1);
        _propsInOrder[last] = newProp;

        // should we just create a new one? Or is resetting ok?
       */

        // First: maybe just replace in place?
        final String key = newProp.getName();
        for (int i = 0; i < _primaryCount; ++i) {
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
    public void replace(SettableBeanProperty newProp)
    {
        /*
        final String key = getPropertyName(newProp);
        int ix = _findIndexInHash(key);
        if (ix < 0) {
            throw new NoSuchElementException("No entry '"+key+"' found, can't replace");
        }
        SettableBeanProperty prop = (SettableBeanProperty) _hashArea[ix];
        _hashArea[ix] = newProp;
        // also, replace in in-order
        _propsInOrder[_findFromOrdered(newProp)] = newProp;
        */

        final String key = newProp.getName();
        for (int i = 0; i < _primaryCount; ++i) {
            if (_propsInOrder[i].getName().equals(key)) {
                _propsInOrder[i] = newProp;
                return;
            }
        }
        throw new NoSuchElementException("No entry '"+key+"' found, can't replace");
    }

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public void remove(SettableBeanProperty propToRm)
    {
        /*
        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_size);
        final String key = getPropertyName(propToRm);
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
                    // 17-Nov-2017, tatu: We used to leave a "hole" here but seems unnecessary
//                    _propsInOrder[_findFromOrdered(prop)] = null;
                    continue;
                }
            }
            props.add(prop);
        }
        if (!found) {
            throw new NoSuchElementException("No entry '"+propToRm.getName()+"' found, can't remove");
        }
        init(props);
        */
        final String key = getPropertyName(propToRm);
        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_size);
        boolean found = false;
        for (SettableBeanProperty prop : _propsInOrder) {
            if (!found) {
                // Important: make sure to lower-case name to match as necessary
                String match = getPropertyName(prop);
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
        _primaryCount = _propsInOrder.length;
    }

    /*
    /**********************************************************
    /* Factory method(s) for helpers
    /**********************************************************
     */

    public FieldNameMatcher constructMatcher(TokenStreamFactory tsf)
    {
        // !!! 11-Nov-2017, tatu: Add aliases

        List<Named> names = Arrays.asList(_propsInOrder);
        // `true` -> yes, they are intern()ed alright
        if (_caseInsensitive) {
            return tsf.constructCIFieldNameMatcher(names, true);
        }
        return tsf.constructFieldNameMatcher(names, true);
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
        List<SettableBeanProperty> result = new ArrayList<>(_primaryCount);
        for (int i = 0; i < _primaryCount; ++i) {
            result.add(_propsInOrder[i]);
        }
        return result;
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

    /**
     * Method similar to {@link #getPrimaryProperties()} but will append aliased
     * properties after primary ones
     */
    public SettableBeanProperty[] getPropertiesWithAliases() {
        // !!! TODO:
        return _propsInOrder;
    }

    // Confining this case insensitivity to this function (and the find method) in case we want to
    // apply a particular locale to the lower case function.  For now, using the default.
    protected final String getPropertyName(SettableBeanProperty prop) {
        if (_caseInsensitive) {
            return prop.getName().toLowerCase();
        }
        return prop.getName();
    }

    /*
    /**********************************************************
    /* Public API, property definition lookup
    /**********************************************************
     */

    public SettableBeanProperty findDefinition(int index)
    {
        // note: will scan the whole area, including primary, secondary and
        // possible spill-area
        /*
        for (int i = 1, end = _hashArea.length; i < end; i += 2) {
            SettableBeanProperty prop = (SettableBeanProperty) _hashArea[i];
            if ((prop != null) && (index == prop.getPropertyIndex())) {
                return prop;
            }
        }
        return null;
        */
        // 17-Nov-2017, tatu: No need to traverse through index is there?
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
     * 
     */
    public SettableBeanProperty findPrimaryDefinition(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Cannot pass null property name");
        }
        for (int i = 0; i < _primaryCount; ++i) {
            if (key.equals(_propsInOrder[i].getName())) {
                return _propsInOrder[i];
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
        if (!_aliasDefs.isEmpty()) {
            sb.append(String.format("(aliases: %s)", _aliasDefs));
        }
        return sb.toString();
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

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

    private Map<String,String> _buildAliasMapping(Map<String,List<PropertyName>> defs)
    {
        if ((defs == null) || defs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,String> aliases = new HashMap<>();
        for (Map.Entry<String,List<PropertyName>> entry : defs.entrySet()) {
            String key = entry.getKey();
            if (_caseInsensitive) {
                key = key.toLowerCase();
            }
            for (PropertyName pn : entry.getValue()) {
                String mapped = pn.getSimpleName();
                if (_caseInsensitive) {
                    mapped = mapped.toLowerCase();
                }
                aliases.put(mapped, key);
            }
        }
        return aliases;
    }

    /*
    private final int _findIndexInHash(String key)
    {
        final int slot = key.hashCode() & _hashMask;     
        
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

    /*
    private final int _findFromOrdered(SettableBeanProperty prop) {
        for (int i = 0, end = _propsInOrder.length; i < end; ++i) {
            if (_propsInOrder[i] == prop) {
                return i;
            }
        }
        throw new IllegalStateException("Illegal state: property '"+prop.getName()+"' missing from _propsInOrder");
    }
*/
}
