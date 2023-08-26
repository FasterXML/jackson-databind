package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.objectid.TestObjectIdWithEquals;
import com.fasterxml.jackson.databind.objectid.TestObjectIdWithPolymorphic;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.databind.BaseTest.verifyException;
import static org.junit.Assert.*;

public class PolymorphicHandlingOverride3943Test
{

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

    @JsonAutoDetect(
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            creatorVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NONE,
            include = JsonTypeInfo.As.WRAPPER_OBJECT,
            property = "_no_type",
            visible = false)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "CLASS_A", value = DataClassA3943.class)
    })
    private static abstract class DataParent3943 {

        @JsonProperty("type")
        @JsonTypeId
        private final DataType3943 type;

        DataParent3943() {
            super();
            this.type = null;
        }

        DataParent3943(final DataType3943 type) {
            super();
            this.type = Objects.requireNonNull(type);
        }

        public DataType3943 getType() {
            return this.type;
        }
    }

    private static final class DataClassA3943 extends DataParent3943 {
        DataClassA3943() {
            super(DataType3943.CLASS_A);
        }
    }

    private enum DataType3943 {
        CLASS_A;
    }

    static class TestCustomResolverBase extends TypeIdResolverBase
    {
        protected final Class<?> superType;
        protected final Class<?> subType;

        public TestCustomResolverBase(Class<?> baseType, Class<?> implType) {
            superType = baseType;
            subType = implType;
        }

        @Override public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CUSTOM; }

        @Override public String idFromValue(Object value) {
            if (superType.isAssignableFrom(value.getClass())) {
                return "*";
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }

        @Override
        public void init(JavaType baseType) { }

        @Override
        public JavaType typeFromId(DatabindContext context, String id)
        {
            if ("*".equals(id)) {
                return TypeFactory.defaultInstance().constructType(subType);
            }
            return null;
        }

        @Override
        public String idFromBaseType() {
            return "xxx";
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="type", defaultImpl=CustomBean3943Impl.class,
            visible=true, requireTypeIdForSubtypes = OptBoolean.FALSE)
    @JsonTypeIdResolver(Custom3943Resolver.class)
    static abstract class CustomBean3943 { }

    static class CustomBean3943Impl extends CustomBean3943 {
        public int x;

        public CustomBean3943Impl() { }
        public CustomBean3943Impl(int x) { this.x = x; }
    }

    static class Custom3943Resolver extends TestCustomResolverBase {
        // yes, static: just for test purposes, not real use
        static List<JavaType> initTypes;

        public Custom3943Resolver() {
            super(CustomBean3943.class, CustomBean3943Impl.class);
        }

        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "_class_to_override")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uri")
    static class Element3943 {
        public URI uri;
        public String name;

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            } else if (object == null || !(object instanceof Element3943)) {
                return false;
            } else {
                Element3943 element = (Element3943) object;
                if (element.uri.toString().equalsIgnoreCase(this.uri.toString())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static abstract class SomeBase3943
    {
        public int value;
        public SomeBase3943 next;

        public SomeBase3943() { this(0); }
        public SomeBase3943(int v) {
            value = v;
        }
    }

    static class SomeImpl3943 extends SomeBase3943
    {
        public int extra;

        public SomeImpl3943() { this(0, 0); }
        public SomeImpl3943(int v, int e) {
            super(v);
            extra = e;
        }
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    @Test
    public void testPolymorphicTypeHandlingViaConfigOverride() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = JsonMapper.builder()
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        assertEquals("{\"_some_type\":\"PolymorphicHandlingOverride3943Test$Squid\"," +
                                "\"id\":\"sqqq\"}",
                m.writeValueAsString(new Squid()));

        Fish fish = (Fish) new Squid();
        assertEquals("{\"_some_type\":\"PolymorphicHandlingOverride3943Test$Squid\",\"id\":\"sqqq\"}",
                m.writeValueAsString(fish));
    }
    
    @Test
    public void testPolymorphicTypeHandlingViaConfigOverride2() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = JsonMapper.builder()
                .withConfigOverride(Base3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        assertEquals("{\"_some_type\":\"PolymorphicHandlingOverride3943Test$Impl3943\"}",
                m.writeValueAsString(new Impl3943()));

        Base3943 base = (Base3943) new Impl3943();
        assertEquals(
                "{\"_some_type\":\"PolymorphicHandlingOverride3943Test$Impl3943\"}",
                m.writeValueAsString(base));
    }


    @Test
    public void testNestedInterfaceSerialization() throws Exception {
        // 1. Override
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "@config-override", Default3943.class, false, true);
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(Interface3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build();

        Interface3943 a = (Interface3943) new A3943();

        assertEquals("{\"@config-override\":\""
                + A3943.class.getDeclaringClass().getSimpleName() + "$A3943\"}", mapper.writeValueAsString(a));
    }

    /**
     * config-override version of {@link JsonTypeInfoCaseInsensitive1983Test#testReadMixedCaseSubclass()}
     */
    @Test
    public void testReadMixedCaseSubclass() throws Exception {
        final String serialised = "{\"Operation\":\"NoTeQ\"}";
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.EXTERNAL_PROPERTY, "Operation", null, false, true);

        // first: mismatch with value unless case-sensitivity disabled:
        try {
            JsonMapper.builder()
                    .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                    .build()
                    .readValue(serialised, Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'NoTeQ'");
        }

        // Type id ("value") mismatch, should work now:
        Filter result = JsonMapper.builder()
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .build()
                .readValue(serialised, Filter.class);
        assertEquals(NotEqual.class, result.getClass());
    }

    /**
     * Config-override version of {@link JsonTypeInfoCaseInsensitive1983Test#testReadMixedCasePropertyName()}
     */
    @Test
    public void testReadMixedCasePropertyName() throws Exception {
        // Arrange
        JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.EXTERNAL_PROPERTY, "Operation", null, false, true);
        ObjectMapper mapper = JsonMapper.builder()
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
        ObjectMapper mapper2 = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .withConfigOverride(Fish.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo))
                .build();
        
        Filter result = mapper2.readValue("{\"oPeRaTioN\":\"notEq\"}", Filter.class);
        
        assertEquals(NotEqual.class, result.getClass());
    }

    /**
     * config-override version of {@link com.fasterxml.jackson.databind.introspect.TestAutoDetect#testAnnotatedFieldIssue2789}
     */
    @Test
    public void testAnnotatedFieldIssue2789WithOverrides() throws Exception {
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "type", null, true, null);
        ObjectMapper mpr = JsonMapper.builder()
                .withConfigOverride(DataParent3943.class,
                        cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        final String json = mpr.writeValueAsString(new DataClassA3943());
        final DataParent3943 copy = mpr.readValue(json, DataParent3943.class);
        assertEquals(DataType3943.CLASS_A, copy.getType());
    }
    
    /**
     * config-override version of {@link TestCustomTypeIdResolver#testCustomTypeIdResolver()}
     */
    @Test
    public void testCustomTypeIdResolverWithOverride() throws Exception
    {
        // config-override
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CUSTOM, JsonTypeInfo.As.WRAPPER_OBJECT,
                null, null, false, null);
        ObjectMapper mpr = JsonMapper.builder()
                .withConfigOverride(CustomBean3943.class,
                        cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // test
        List<JavaType> types = new ArrayList<JavaType>();
        Custom3943Resolver.initTypes = types;
        String json = mpr.writeValueAsString(new CustomBean3943[] { new CustomBean3943Impl(28) });
        assertEquals("[{\"*\":{\"x\":28}}]", json);
        assertEquals(1, types.size());
        assertEquals(CustomBean3943.class, types.get(0).getRawClass());

        types = new ArrayList<JavaType>();
        Custom3943Resolver.initTypes = types;
        CustomBean3943[] result = mpr.readValue(json, CustomBean3943[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(28, ((CustomBean3943Impl) result[0]).x);
        assertEquals(1, types.size());
        assertEquals(CustomBean3943.class, types.get(0).getRawClass());
    }

    /**
     * config-override version of {@link TestObjectIdWithEquals#testEqualObjectIdsExternal()}
     */
    @Test
    public void testEqualObjectIdsExternalWithOverrides() throws Exception
    {
        Element3943 element = new Element3943();
        element.uri = URI.create("URI");
        element.name = "Element39431";

        Element3943 element2 = new Element3943();
        element2.uri = URI.create("URI");
        element2.name = "Element39432";

        // 12-Nov-2015, tatu: array works fine regardless of Type Erasure, but if using List,
        //   must provide additional piece of type info
//        Element3943[] input = new Element3943[] { element, element2 };
        List<Element3943> input = Arrays.asList(element, element2);

        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS, JsonTypeInfo.As.PROPERTY,
                "@class", null, false, null);
        ObjectMapper mapper = JsonMapper.builder()
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID)
                .withConfigOverride(Element3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

//        String json = mapper.writeValueAsString(input);
        String json = mapper.writerFor(new TypeReference<List<Element3943>>() { })
                .writeValueAsString(input);

        Element3943[] output = mapper.readValue(json, Element3943[].class);
        assertNotNull(output);
        assertEquals(2, output.length);
    }

    /**
     * config-override version of {@link TestObjectIdWithPolymorphic#testPolymorphicRoundtrip()}
     */
    @Test
    public void testWithConfigOverrides() throws Exception {
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS, JsonTypeInfo.As.PROPERTY,
                "@class", null, false, null);
        ObjectMapper mpr = JsonMapper.builder()
                .withConfigOverride(Base3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // create simple 2 node loop:
        SomeImpl3943 in1 = new SomeImpl3943(1, 2);
        in1.next = new SomeImpl3943(3, 4);
        in1.next.next = in1;

        String json = mpr.writeValueAsString(in1);

        // then bring back...
        SomeBase3943 result0 = mpr.readValue(json, SomeBase3943.class);
        assertNotNull(result0);
        assertSame(SomeImpl3943.class, result0.getClass());
        SomeImpl3943 result = (SomeImpl3943) result0;
        assertEquals(1, result.value);
        assertEquals(2, result.extra);
        SomeImpl3943 nextResult = (SomeImpl3943) result.next;
        assertEquals(3, nextResult.value);
        assertEquals(4, nextResult.extra);
        assertSame(result, nextResult.next);
    } 
}
