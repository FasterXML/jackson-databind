package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParsingContextExtTypeId2747Test extends DatabindTestUtil {
    static class Wrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public Tag wrapped;

        public String type;
    }

    // Creator-based wrapper: value flows through `ExternalTypeHandler.complete(p, ctxt, buffer, creator)`
    // which uses `_deserialize` (not `_deserializeAndSet`).
    static class CreatorWrapper {
        final String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        @JsonSubTypes(@JsonSubTypes.Type(Location.class))
        final Tag wrapped;

        @JsonCreator
        CreatorWrapper(@JsonProperty("type") String type,
                @JsonProperty("wrapped") Tag wrapped) {
            this.type = type;
            this.wrapped = wrapped;
        }
    }

    // defaultImpl wrapper: when type-id property is missing, complete() resolves to
    // the default type via `extProp.getDefaultTypeId(ctxt)` and feeds that id through
    // the same fast path.
    static class DefaultImplWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                defaultImpl = Location.class)
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
    @Test
    void locationAccessWithExtTypeId() throws Exception {
        ObjectReader objectReader = newJsonMapper().readerFor(Wrapper.class);

        // Scalar value payload
        Wrapper wrapper = objectReader.readValue("{" +
                "\"type\":\"location\"," +
                "\"wrapped\": 1" +
                "}");
        assertEquals("/wrapped", ((Location) wrapper.wrapped).value);

        // Structured (Object) value payload: previously threw MismatchedInputException
        // "expected closing END_ARRAY" because of the synthetic WRAPPER_ARRAY.
        wrapper = objectReader.readValue("{" +
                "\"type\":\"location\"," +
                "\"wrapped\": {}" +
                "}");
        assertEquals("/wrapped", ((Location) wrapper.wrapped).value);

        // Type-id property appearing AFTER value: the value is buffered first and
        // replayed only once the type id is known. Without pinning the TokenBuffer's
        // parent context at buffering time, the outer parser's `currentName` has
        // advanced to "type" by replay time and leaks into the deserializer's view.
        wrapper = objectReader.readValue("{" +
                "\"wrapped\": 1," +
                "\"type\":\"location\"" +
                "}");
        assertEquals("/wrapped", ((Location) wrapper.wrapped).value);

        wrapper = objectReader.readValue("{" +
                "\"wrapped\": {}," +
                "\"type\":\"location\"" +
                "}");
        assertEquals("/wrapped", ((Location) wrapper.wrapped).value);
    }

    // [databind#2747]: creator path routes through `_deserialize()` rather than
    // `_deserializeAndSet()` — verify the fix applies there too.
    @Test
    void locationAccessWithExtTypeIdCreator() throws Exception {
        ObjectReader objectReader = newJsonMapper().readerFor(CreatorWrapper.class);

        CreatorWrapper w = objectReader.readValue("{" +
                "\"type\":\"location\"," +
                "\"wrapped\": 1" +
                "}");
        assertEquals("/wrapped", ((Location) w.wrapped).value);

        w = objectReader.readValue("{" +
                "\"wrapped\": {}," +
                "\"type\":\"location\"" +
                "}");
        assertEquals("/wrapped", ((Location) w.wrapped).value);
    }

    // [databind#2747]: defaultImpl path — type-id property missing in JSON, type id
    // is resolved via `extProp.getDefaultTypeId(ctxt)` and fed through the fast path.
    @Test
    void locationAccessWithExtTypeIdDefaultImpl() throws Exception {
        ObjectReader objectReader = newJsonMapper().readerFor(DefaultImplWrapper.class);

        DefaultImplWrapper w = objectReader.readValue("{\"wrapped\": {}}");
        assertEquals("/wrapped", ((Location) w.wrapped).value);
    }
}
