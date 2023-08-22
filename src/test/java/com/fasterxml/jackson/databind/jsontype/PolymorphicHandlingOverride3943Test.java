package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class PolymorphicHandlingOverride3943Test extends BaseMapTest {

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
    }

    static class Impl3943 extends Base3943 {
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "Operation")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AInterface3943.class),
            @JsonSubTypes.Type(value = BInterface3943.class),
    })
    static interface Interface3943 {
    }

    static interface AInterface3943 extends Interface3943 {
    }

    static interface BInterface3943 extends Interface3943 {
    }

    static class A3943 implements AInterface3943 {
    }

    static class Default3943 implements Interface3943 {
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

        Fish fish = (Fish) new Squid();
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverride3943Test$Squid','id':'sqqq'}"),
                m.writeValueAsString(fish));
    }

    public void testPolymorphicTypeHandlingViaConfigOverride2() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Base3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // Assert
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverride3943Test$Impl3943'}"),
                m.writeValueAsString(new Impl3943()));

        Base3943 base = (Base3943) new Impl3943();
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverride3943Test$Impl3943'}"),
                m.writeValueAsString(base));
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
        // Arrange
        JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.EXTERNAL_PROPERTY, "Operation", null, false, true);
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build();

        // Act & Assert
        try {
            mapper.readValue("{\"oPeRaTioN\":\"notEq\"}", Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property");
        }

        // Type property name mismatch (but value match); should work:
        ObjectMapper mapper2 = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build();
        
        Filter result = mapper2.readValue("{\"oPeRaTioN\":\"notEq\"}", Filter.class);
        
        assertEquals(NotEqual.class, result.getClass());
    }


    public void testNestedInterfaceSerialization() throws Exception {
        // 1. Override
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "@config-override", Default3943.class, false, true);
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Interface3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build();

        Interface3943 a = (Interface3943) new A3943();

        assertEquals("{\"@config-override\":\""
                + A3943.class.getDeclaringClass().getSimpleName() + "$A3943\"}", mapper.writeValueAsString(a));
    }
}
