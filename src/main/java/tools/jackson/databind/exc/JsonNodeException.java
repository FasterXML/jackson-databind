package tools.jackson.databind.exc;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;

public class JsonNodeException
    extends DatabindException
{
    private static final long serialVersionUID = 3L;

    protected final JsonNode _node;

    protected JsonNodeException(JsonNode node, String message) {
        super(message);
        _node = node;
    }

    public static JsonNodeException from(JsonNode node, String message) {
        return new JsonNodeException(node, message);
    }

    public static JsonNodeException from(JsonNode node,
            String message, Object... args) {
        return new JsonNodeException(node,
                String.format(message, args));
    }

    public JsonNode getNode() {
        return _node;
    }
}
