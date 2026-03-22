package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Deserializer for the {@link java.lang.ThreadGroup} class: due to limited access,
 * will only try to extract {@code "name"} property and ignores everything else.
 * This to match automatic serialization by Jackson which does write out
 * all accessible properties.
 *
 * @since 2.19
 */
public class ThreadGroupDeserializer
    extends StdNodeBasedDeserializer<ThreadGroup>
{
    protected ThreadGroupDeserializer() {
        super(ThreadGroup.class);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public ThreadGroup convert(JsonNode root, DeserializationContext ctxt) throws IOException {
        String name = root.path("name").asText();
        if (name == null) {
            name = "";
        }
        return new ThreadGroup(name);
    }
}
