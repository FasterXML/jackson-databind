package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

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

   static class LocationDeserializer extends ValueDeserializer<Location>
   {
        @Override
        public Location deserialize(JsonParser p, DeserializationContext ctxt)
        {
            p.skipChildren();
            return new Location(getCurrentLocationAsString(p));
        }
    }

   static String getCurrentLocationAsString(JsonParser p)
   {
       // This suffices to give actual path
       return p.streamReadContext().pathAsPointer().toString();
   }

   // [databind#2747]
    @JacksonTestFailureExpected
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
