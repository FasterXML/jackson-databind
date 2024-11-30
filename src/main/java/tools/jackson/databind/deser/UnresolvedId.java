package tools.jackson.databind.deser;

import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.util.ClassUtil;

/**
 * Helper class for {@link UnresolvedForwardReference}, to contain information about unresolved ids.
 *
 * @author pgelinas
 */
public class UnresolvedId {
    private final Object _id;
    private final TokenStreamLocation _location;
    private final Class<?> _type;

    public UnresolvedId(Object id, Class<?> type, TokenStreamLocation where) {
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
    public TokenStreamLocation getLocation() { return _location; }

    @Override
    public String toString() {
        return String.format("Object id [%s] (for %s) at %s", _id,
                ClassUtil.nameOf(_type), _location);
    }
}
