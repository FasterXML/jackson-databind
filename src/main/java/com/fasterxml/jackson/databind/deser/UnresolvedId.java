package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class for {@link UnresolvedForwardReference}, to contain information about unresolved ids.
 *
 * @author pgelinas
 */
public class UnresolvedId {
    private final Object _id;
    private final JsonLocation _location;
    private final Class<?> _type;

    public UnresolvedId(Object id, Class<?> type, JsonLocation where) {
        _id = id;
        _type = type;
        _location = where;
    }

    /**
     * The id which is unresolved.
     */
    public Object getId() { return _id; }

    /**
     * The type of object which was expected.
     */
    public Class<?> getType() { return _type; }
    public JsonLocation getLocation() { return _location; }

    @Override
    public String toString() {
        return String.format("Object id [%s] (for %s) at %s", _id,
                ClassUtil.nameOf(_type), _location);
    }
}
