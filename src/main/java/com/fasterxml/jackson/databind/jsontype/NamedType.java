package com.fasterxml.jackson.databind.jsontype;

import java.util.Objects;

/**
 * Simple container class for types with optional logical name, used
 * as external identifier
 */
public final class NamedType implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final Class<?> _class;
    protected final int _hashCode;

    protected String _name;

    public NamedType(Class<?> c) { this(c, null); }

    public NamedType(Class<?> c, String name) {
        _class = c;
        _hashCode = c.getName().hashCode() + ((name == null) ? 0 : name.hashCode());
        setName(name);
    }

    public Class<?> getType() { return _class; }
    public String getName() { return _name; }
    public void setName(String name) { _name = (name == null || name.isEmpty()) ? null : name; }

    public boolean hasName() { return _name != null; }

    /**
     * Equality is defined based on class and name
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        NamedType other = (NamedType)o;
        return (_class == other._class)
                && Objects.equals(_name, other._name);
    }

    @Override
    public int hashCode() { return _hashCode; }

    @Override
    public String toString() {
    	return "[NamedType, class "+_class.getName()+", name: "
    	        +(_name == null ? "null" :("'"+_name+"'"))+"]";
    }
}
