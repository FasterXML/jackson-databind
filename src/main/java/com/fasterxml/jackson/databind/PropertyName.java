package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.InternCache;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Simple value class used for containing names of properties as defined
 * by annotations (and possibly other configuration sources).
 *
 * @since 2.1
 */
public class PropertyName
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L; // 2.5

    private final static String _USE_DEFAULT = "";
    private final static String _NO_NAME = "";

    /**
     * Special placeholder value that indicates that name to use should be
     * based on the standard heuristics. This can be different from returning
     * null, as null means "no information available, whereas this value
     * indicates explicit defaulting.
     */
    public final static PropertyName USE_DEFAULT = new PropertyName(_USE_DEFAULT, null);

    /**
     * Special placeholder value that indicates that there is no name associated.
     * Exact semantics to use (if any) depend on actual annotation in use, but
     * commonly this value disables behavior for which name would be needed.
     */
    public final static PropertyName NO_NAME = new PropertyName(new String(_NO_NAME), null);

    /**
     * Basic name of the property.
     */
    protected final String _simpleName;

    /**
     * Additional namespace, for formats that have such concept (JSON
     * does not, XML does, for example).
     */
    protected final String _namespace;

    /**
     * Lazily-constructed efficient representation of the simple name.
     *<p>
     * NOTE: not defined as volatile to avoid performance problem with
     * concurrent access in multi-core environments; due to statelessness
     * of {@link SerializedString} at most leads to multiple instantiations.
     *
     * @since 2.4
     */
    protected SerializableString _encodedSimple;

    public PropertyName(String simpleName) {
        this(simpleName, null);
    }

    public PropertyName(String simpleName, String namespace)
    {
        _simpleName = ClassUtil.nonNullString(simpleName);
        _namespace = namespace;
    }

    // To support JDK serialization, recovery of Singleton instance
    protected Object readResolve() {
        if (_namespace == null) {
            if (_simpleName == null || _USE_DEFAULT.equals(_simpleName)) {
                return USE_DEFAULT;
            }
            // 30-Oct-2016, tatu: I don't see how this could ever occur...
            //     or how to distinguish USE_DEFAULT/NO_NAME from serialized
            /*
            if (_simpleName.equals(_NO_NAME)) {
                return NO_NAME;
            }
            */
        }
        return this;
    }

    /**
     * @since 2.6
     */
    public static PropertyName construct(String simpleName)
    {
        if (simpleName == null || simpleName.isEmpty()) {
            return USE_DEFAULT;
        }
        return new PropertyName(InternCache.instance.intern(simpleName), null);
    }

    public static PropertyName construct(String simpleName, String ns)
    {
        if (simpleName == null) {
            simpleName = "";
        }
        if (ns == null && simpleName.isEmpty()) {
            return USE_DEFAULT;
        }
        return new PropertyName(InternCache.instance.intern(simpleName), ns);
    }

    public PropertyName internSimpleName()
    {
        if (_simpleName.isEmpty()) { // empty String is canonical already
            return this;
        }
        String interned = InternCache.instance.intern(_simpleName);
        if (interned == _simpleName) { // was already interned
            return this;
        }
        return new PropertyName(interned, _namespace);
    }

    /**
     * Fluent factory method for constructing an instance with different
     * simple name.
     */
    public PropertyName withSimpleName(String simpleName)
    {
        if (simpleName == null) {
            simpleName = "";
        }
        if (simpleName.equals(_simpleName)) {
            return this;
        }
        return new PropertyName(simpleName, _namespace);
    }

    /**
     * Fluent factory method for constructing an instance with different
     * namespace.
     */
    public PropertyName withNamespace(String ns) {
        if (ns == null) {
            if (_namespace == null) {
                return this;
            }
        } else if (ns.equals(_namespace)) {
            return this;
        }
        return new PropertyName(_simpleName, ns);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public String getSimpleName() {
        return _simpleName;
    }

    /**
     * Accessor that may be used to get lazily-constructed efficient
     * representation of the simple name.
     *
     * @since 2.4
     */
    public SerializableString simpleAsEncoded(MapperConfig<?> config) {
        SerializableString sstr = _encodedSimple;
        if (sstr == null) {
            if (config == null) {
                sstr = new SerializedString(_simpleName);
            } else {
                sstr = config.compileString(_simpleName);
            }
            _encodedSimple = sstr;
        }
        return sstr;
    }

    public String getNamespace() {
        return _namespace;
    }

    public boolean hasSimpleName() {
        return !_simpleName.isEmpty();
    }

    /**
     * @since 2.3
     */
    public boolean hasSimpleName(String str) {
        // _simpleName never null so...
        return _simpleName.equals(str);
    }

    public boolean hasNamespace() {
        return _namespace != null;
    }

    /**
     * Method that is basically equivalent of:
     *<pre>
     *   !hasSimpleName() &lt;&lt; !hasNamespace();
     *</pre>
     *
     * @since 2.4
     */
    public boolean isEmpty() {
        return (_namespace == null) && (_simpleName.isEmpty());
    }

    /*
    /**********************************************************
    /* Std method overrides
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        /* 13-Nov-2012, tatu: by default, require strict type equality.
         *   Re-evaluate if this becomes an issue.
         */
        if (o.getClass() != getClass()) return false;
        // 13-Nov-2012, tatu: Should we have specific rules on matching USE_DEFAULT?
        //   (like, it only ever matching exact instance)
        //   If we did, would need to check symmetrically; that is, if either 'this'
        //   or 'o' was USE_DEFAULT, both would have to be.
        PropertyName other = (PropertyName) o;
        if (_simpleName == null) {
            if (other._simpleName != null) return false;
        } else if (!_simpleName.equals(other._simpleName)) {
            return false;
        }
        if (_namespace == null) {
            return (null == other._namespace);
        }
        return _namespace.equals(other._namespace);
    }

    @Override
    public int hashCode() {
        if (_namespace == null) {
            return _simpleName.hashCode();
        }
        return _namespace.hashCode() ^  _simpleName.hashCode();
    }

    @Override
    public String toString() {
        if (_namespace == null) {
            return _simpleName;
        }
        return "{"+_namespace + "}" + _simpleName;
    }
}
