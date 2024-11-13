package tools.jackson.databind.deser.bean;

import java.util.*;

import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.Named;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.util.IgnorePropertiesUtil;
import tools.jackson.databind.util.NameTransformer;

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
    implements Iterable<SettableBeanProperty>
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Array of properties in the exact order they were handed in. This is
     * used by as-array serialization, deserialization.
     * Contains both primary properties (first <code>_primaryCount</code>
     * entries) and possible aliased mappings
     */
    private SettableBeanProperty[] _propsInOrder;

    /**
     * Configuration of alias mappings, if any (`null` if none),
     * aligned with properties in <code>_propsInOrder</code>
     */
    private final PropertyName[][] _aliasDefs;

    private final Locale _locale;

    private final boolean _caseInsensitive;

    /*
    /**********************************************************************
    /* Lookup index information constructed
    /**********************************************************************
     */

    private transient PropertyNameMatcher _nameMatcher;

    /**
     * Lazily instantiated array of properties mapped from lookup index, in which
     * first entries are same as in <code>_propsInOrder</code> followed by alias
     * mappings.
     */
    private transient SettableBeanProperty[] _propsWithAliases;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    /**
     * @param caseInsensitive Whether property name matching should case-insensitive or not
     * @param props Sequence of primary properties to index
     * @param aliasDefs Alias mappings, if any (null if none)
     * @param assignIndexes Whether to assign indices to property entities or not
     */
    protected BeanPropertyMap(Collection<SettableBeanProperty> props,
            PropertyName[][] aliasDefs,
            Locale locale, boolean caseInsensitive,
            boolean assignIndexes)
    {
        _locale = locale;
        _caseInsensitive = caseInsensitive;
        _aliasDefs = aliasDefs;
        _propsInOrder = props.toArray(new SettableBeanProperty[0]);
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
        _locale = base._locale;
        _caseInsensitive = caseInsensitive;
        _aliasDefs = base._aliasDefs;

        // 16-May-2016, tatu: Alas, not enough to just change flag, need to re-init as well.
        _propsInOrder = Arrays.copyOf(base._propsInOrder, base._propsInOrder.length);
//        init(Arrays.asList(_propsInOrder));
    }

    public static BeanPropertyMap construct(MapperConfig<?> config,
            Collection<SettableBeanProperty> props,
            PropertyName[][] aliases,
            boolean caseInsensitive)
    {
        return new BeanPropertyMap(props, aliases,
                config.getLocale(), caseInsensitive, true);
    }

    /*
    /**********************************************************************
    /* "Mutant factory" methods
    /**********************************************************************
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
        return new BeanPropertyMap(newProps, _aliasDefs, _locale, _caseInsensitive, false);
    }

    /**
     * Mutant factory method for constructing a map where all entries use given
     * prefix
     */
    public BeanPropertyMap renameAll(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        if (transformer == null || (transformer == NameTransformer.NOP)) {
            return this;
        }
        // Try to retain insertion ordering as well
        final int len = _propsInOrder.length;
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(_propsInOrder.length);
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty orig = _propsInOrder[i];
            SettableBeanProperty prop = orig.unwrapped(ctxt, transformer);
            newProps.add(prop);
        }
        // 26-Feb-2017, tatu: Probably SHOULD handle renaming wrt Aliases?
        // NOTE: do NOT try reassigning indexes of properties; number doesn't change

        // !!! 18-Nov-2017, tatu: Should try recreating PropertyNameMatcher here but...
        return new BeanPropertyMap(newProps, _aliasDefs, _locale, _caseInsensitive, false)
                .initMatcher(ctxt.tokenStreamFactory());
    }

    /**
     * Mutant factory method that will use this instance as the base, and
     * construct an instance that is otherwise same except for excluding
     * properties with specified names.
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
     */
    public BeanPropertyMap withoutProperties(Collection<String> toExclude, Collection<String> toInclude)
    {
        if ((toExclude == null) || toExclude.isEmpty()) {
            if (toInclude == null) {
                return this;
            }
            toExclude = Collections.emptySet();
        }
        final int len = _propsInOrder.length;
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(len);

        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _propsInOrder[i];
            // 23-Jul-2020, tatu: Earlier comment from 2.x suggested `prop` could be null,
            //    checked, skipped, if so... but no more null checks
            if (!toExclude.contains(prop.getName())) {
                if (!IgnorePropertiesUtil.shouldIgnore(prop.getName(), toExclude, toInclude)) {
                    newProps.add(prop);
                }
            }
        }
        // should we try to re-index? Apparently no need
        // 17-Nov-2017, tatu: do NOT try to change indexes since this could lead to discrepancies
        //    (unless we actually copy property instances)
        return new BeanPropertyMap(newProps, _aliasDefs, _locale, _caseInsensitive, false);
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
        final String key = propToRm.getName();
        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_propsInOrder.length);
        boolean found = false;
        for (SettableBeanProperty prop : _propsInOrder) {
            if (!found) {
                String match = prop.getName();
                if (found = match.equals(key)) {
                    continue;
                }
            }
            props.add(prop);
        }
        if (!found) {
            throw new NoSuchElementException("No entry '"+propToRm.getName()+"' found, can't remove");
        }
        _propsInOrder = props.toArray(new SettableBeanProperty[0]);
    }

    /*
    /**********************************************************************
    /* Factory method(s) for helpers
    /**********************************************************************
     */

    public BeanPropertyMap initMatcher(TokenStreamFactory tsf)
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
            _propsWithAliases = allProps.toArray(new SettableBeanProperty[0]);
        }
        // `true` -> yes, they are intern()ed alright
        if (_caseInsensitive) {
            _nameMatcher = tsf.constructCINameMatcher(names, true, _locale);
        } else {
            _nameMatcher = tsf.constructNameMatcher(names, true);
        }
        return this;
    }

    public PropertyNameMatcher getNameMatcher() { return _nameMatcher; }
    public SettableBeanProperty[] getNameMatcherProperties() { return _propsWithAliases; }

    /*
    /**********************************************************************
    /* Public API, simple accessors
    /**********************************************************************
     */

    public int size() { return _propsInOrder.length; }

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
    /**********************************************************************
    /* Public API, property definition lookup
    /**********************************************************************
     */

    public SettableBeanProperty findDefinition(int index)
    {
        for (SettableBeanProperty prop : _propsInOrder) {
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
    public SettableBeanProperty findDefinition(String key)
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
    /**********************************************************************
    /* Std method overrides
    /**********************************************************************
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
            sb.append(String.format("(aliases: %s)", _aliasDefs.length));
        }
        return sb.toString();
    }
}
