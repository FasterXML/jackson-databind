package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParsingContextExtTypeId2747Test extends DatabindTestUtil {
    static class Wrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public Tag wrapped;

        public String type;
    }

    @JsonSubTypes(@JsonSubTypes.Type(Location.class))
    interface Tag {
    }

    @JsonTypeName("location")
    @JsonDeserialize(using = LocationDeserializer.class)
    static class Location implements Tag {
        String value;

        protected Location() {
        }

        Location(String v) {
            value = v;
        }
    }

    static class LocationDeserializer extends JsonDeserializer<Location> {
        @Override
        public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            p.skipChildren();
            return new Location(getCurrentLocationAsString(p));
        }
    }

    static String getCurrentLocationAsString(JsonParser p) {
        // This suffices to give actual path
        return p.getParsingContext().pathAsPointer().toString();
    }

    // [databind#2747]
    @Test
    void locationAccessWithExtTypeId() throws Exception {
        ObjectReader objectReader = newJsonMapper().readerFor(Wrapper.class);

        Wrapper wrapper = objectReader.readValue("{" +
                "\"type\":\"location\"," +
                "\"wrapped\": 1" +
                "}");
        // expecting wrapper.wrapped.value == "wrapped" but is "wrapped[1]"
        // due to way `ExternalTypeHandler` exposes value as if "wrapper-array" was used for
        // type id, value
        assertEquals("/wrapped", ((Location) wrapper.wrapped).value);
    }
}
