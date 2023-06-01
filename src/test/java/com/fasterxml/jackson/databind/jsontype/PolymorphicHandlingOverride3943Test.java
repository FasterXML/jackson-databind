package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class PolymorphicHandlingOverride3943Test extends BaseMapTest {
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
    @JsonSubTypes({@JsonSubTypes.Type(Squid.class)})
    static abstract class Fish {
        public String id;
    }

    static class Squid extends Fish {
        public Squid() {
            this.id = "sqqq";
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY, property = "NOTHING")
    @JsonSubTypes({@JsonSubTypes.Type(Impl3943.class)})
    static abstract class Base3943 {
        public String id;
    }

    static class Impl3943 extends Base3943 {
        public Impl3943() {
            this.id = "someImpl";
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "Operation")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Equal.class, name = "eq"),
            @JsonSubTypes.Type(value = NotEqual.class, name = "notEq"),
    })
    static abstract class Filter {
    }

    static class Equal extends Filter {
    }

    static class NotEqual extends Filter {
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testPolymorphicTypeHandlingViaConfigOverride() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // Assert
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverride3943Test$Squid','id':'sqqq'}"),
                m.writeValueAsString(new Squid()));
    }

    public void testPolymorphicTypeHandlingViaConfigOverride2() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Base3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // Assert
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverride3943Test$Impl3943','id':'someImpl'}"),
                m.writeValueAsString(new Impl3943()));
    }

    /**
     * Originally from {@link JsonTypeInfoCaseInsensitive1983Test}
     */
    public void testReadMixedCaseSubclass() throws Exception {
        final String serialised = "{\"Operation\":\"NoTeQ\"}";
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.EXTERNAL_PROPERTY, "Operation", null, false, true);

        // first: mismatch with value unless case-sensitivity disabled:
        try {
            jsonMapperBuilder()
                    .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                    .build()
                    .readValue(serialised, Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'NoTeQ'");
        }

        // Type id ("value") mismatch, should work now:
        Filter result = jsonMapperBuilder()
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .build()
                .readValue(serialised, Filter.class);
        assertEquals(NotEqual.class, result.getClass());
    }

    /**
     * Originally from {@link JsonTypeInfoCaseInsensitive1983Test}
     */
    public void testReadMixedCasePropertyName() throws Exception {
        final String serialised = "{\"oPeRaTioN\":\"notEq\"}";
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.EXTERNAL_PROPERTY, "Operation", null, false, true);

        try {
            jsonMapperBuilder()
                    .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                    .build()
                    .readValue(serialised, Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property");
        }

        // Type property name mismatch (but value match); should work:
        Filter result = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build()
                .readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }
}
