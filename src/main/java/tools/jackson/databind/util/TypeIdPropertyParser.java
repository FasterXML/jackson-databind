package tools.jackson.databind.util;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.util.JsonParserDelegate;

/**
 * Internal parser delegate that exposes an already consumed type id property
 * to the deserializer binding the object.
 *
 * @since 3.4
 */
public final class TypeIdPropertyParser extends JsonParserDelegate
{
    private final TokenStreamContext _objectContext;
    private final JsonToken _firstToken;
    private String _propertyName;

    public TypeIdPropertyParser(JsonParser parser, String propertyName) {
        super(parser);
        _objectContext = parser.streamReadContext();
        _firstToken = parser.currentToken();
        _propertyName = propertyName;
    }

    /**
     * Returns the property name once, at the start of binding its object.
     */
    public String takePropertyName() {
        if (_propertyName == null || streamReadContext() != _objectContext || currentToken() != _firstToken) {
            return null;
        }
        String propertyName = _propertyName;
        _propertyName = null;
        return propertyName;
    }
}
