package tools.jackson.databind.deser.jdk;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdNodeBasedDeserializer;

/**
 * Deserializer for the {@link java.lang.ThreadGroup} class: due to limited access,
 * will only try to extract {@code "name"} property and ignores everything else.
 * This to match automatic serialization by Jackson which does write out
 * all accessible properties.
 */
public class ThreadGroupDeserializer
    extends StdNodeBasedDeserializer<ThreadGroup>
{
    protected ThreadGroupDeserializer() {
        super(ThreadGroup.class);
    }

    @Override
    public ThreadGroup convert(JsonNode root, DeserializationContext ctxt) {
        String name = root.path("name").asString("");
        return new ThreadGroup(name);
    }
}
