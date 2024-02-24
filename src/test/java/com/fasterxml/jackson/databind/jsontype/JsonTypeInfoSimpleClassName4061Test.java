package com.fasterxml.jackson.databind.jsontype;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.impl.SimpleNameIdResolver;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for <a href="https://github.com/FasterXML/jackson-databind/issues/4061">
 * [databind#4061] Add JsonTypeInfo.Id.SIMPLE_NAME
 * 
 * @since 2.16
 */
public class JsonTypeInfoSimpleClassName4061Test extends DatabindTestUtil
{

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.SIMPLE_NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = InnerSub4061A.class),
            @JsonSubTypes.Type(value = InnerSub4061B.class)
    })
    static class InnerSuper4061 { }

    static class InnerSub4061A extends InnerSuper4061 { }

    static class InnerSub4061B extends InnerSuper4061 { }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.MINIMAL_CLASS)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MinimalInnerSub4061A.class),
            @JsonSubTypes.Type(value = MinimalInnerSub4061B.class)
    })
    static class MinimalInnerSuper4061 { }

    static class MinimalInnerSub4061A extends MinimalInnerSuper4061 { }

    static class MinimalInnerSub4061B extends MinimalInnerSuper4061 { }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.SIMPLE_NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MixedSub4061A.class),
            @JsonSubTypes.Type(value = MixedSub4061B.class)
    })
    static class MixedSuper4061 { }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.MINIMAL_CLASS)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MixedMinimalSub4061A.class),
            @JsonSubTypes.Type(value = MixedMinimalSub4061B.class)
    })
    static class MixedMinimalSuper4061 { }

    static class Root {
        @JsonMerge
        public MergeChild child;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MergeChildA.class, name = "MergeChildA"),
            @JsonSubTypes.Type(value = MergeChildB.class, name = "MergeChildB")
    })
    static abstract class MergeChild {
    }

    static class MergeChildA extends MergeChild {
        public String name;
    }

    static class MergeChildB extends MergeChild {
        public String code;
    }

    static class PolyWrapperForAlias {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = AliasBean.class,name = "ab")})
        public Object value;

        protected PolyWrapperForAlias() { }
        
        public PolyWrapperForAlias(Object v) { value = v; }
    }

    static class AliasBean {
        @JsonAlias({ "nm", "Name" })
        public String name;
        int _xyz;
        int _a;

        @JsonCreator
        public AliasBean(@JsonProperty("a") @JsonAlias("A") int a) {
            _a = a;
        }

        @JsonAlias({ "Xyz" })
        public void setXyz(int x) {
            _xyz = x;
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.SIMPLE_NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DuplicateSubClass.class),
            @JsonSubTypes.Type(value = com.fasterxml.jackson.databind.jsontype.DuplicateSubClass.class)
    })
    static class DuplicateSuperClass { }
    
    static class DuplicateSubClass extends DuplicateSuperClass { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = newJsonMapper();

    // inner class that has contains dollar sign
    @Test
    public void testInnerClass() throws Exception
    {
        String jsonStr = a2q("{'@type':'InnerSub4061A'}");
        
        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new InnerSub4061A()));
        
        // deser <- breaks!
        InnerSuper4061 bean = MAPPER.readValue(jsonStr, InnerSuper4061.class);
        assertInstanceOf(InnerSuper4061.class, bean);
    }

    // inner class that has contains dollar sign
    @Test
    public void testMinimalInnerClass() throws Exception
    {
        String jsonStr = a2q("{'@c':'.JsonTypeInfoSimpleClassName4061Test$MinimalInnerSub4061A'}");
        
        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new MinimalInnerSub4061A()));
        
        // deser <- breaks!
        MinimalInnerSuper4061 bean = MAPPER.readValue(jsonStr, MinimalInnerSuper4061.class);
        assertInstanceOf(MinimalInnerSuper4061.class, bean);
        assertNotNull(bean);
    }

    // Basic : non-inner class, without dollar sign
    @Test
    public void testBasicClass() throws Exception
    {
        String jsonStr = a2q("{'@type':'BasicSub4061A'}");
        
        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new BasicSub4061A()));
        
        // deser
        BasicSuper4061 bean = MAPPER.readValue(jsonStr, BasicSuper4061.class);
        assertInstanceOf(BasicSuper4061.class, bean);
        assertInstanceOf(BasicSub4061A.class, bean);

    }
    
    // Mixed SimpleClassName : parent as inner, subtype as basic
    @Test
    public void testMixedClass() throws Exception
    {
        String jsonStr = a2q("{'@type':'MixedSub4061A'}");
        
        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new MixedSub4061A()));
        
        // deser
        MixedSuper4061 bean = MAPPER.readValue(jsonStr, MixedSuper4061.class);
        assertInstanceOf(MixedSuper4061.class, bean);
        assertInstanceOf(MixedSub4061A.class, bean);
    }
    
    // Mixed MinimalClass : parent as inner, subtype as basic
    @Test
    public void testMixedMinimalClass() throws Exception
    {
        String jsonStr = a2q("{'@c':'.MixedMinimalSub4061A'}");
        
        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new MixedMinimalSub4061A()));
        
        // deser
        MixedMinimalSuper4061 bean = MAPPER.readValue(jsonStr, MixedMinimalSuper4061.class);
        assertInstanceOf(MixedMinimalSuper4061.class, bean);
        assertInstanceOf(MixedMinimalSub4061A.class, bean);
    }

    @Test
    public void testPolymorphicNewObject() throws Exception
    {
        String jsonStr = "{\"child\": { \"@type\": \"MergeChildA\", \"name\": \"I'm child A\" }}";
        
        Root root = MAPPER.readValue(jsonStr, Root.class);
        
        assertTrue(root.child instanceof MergeChildA);
        assertEquals("I'm child A", ((MergeChildA) root.child).name);
    }

    // case insenstive type name
    @Test
    public void testPolymorphicNewObjectCaseInsensitive() throws Exception
    {
        String jsonStr = "{\"child\": { \"@type\": \"mergechilda\", \"name\": \"I'm child A\" }}";
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .build();

        Root root = mapper.readValue(jsonStr, Root.class);
        
        assertTrue(root.child instanceof MergeChildA);
        assertEquals("I'm child A", ((MergeChildA) root.child).name);
    }

    @Test
    public void testPolymorphicNewObjectUnknownTypeId() throws Exception
    {
        try {
            MAPPER.readValue("{\"child\": { \"@type\": \"UnknownChildA\", \"name\": \"I'm child A\" }}", Root.class);    
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'UnknownChildA' as a subtype of");
        }
    }

    @Test
    public void testAliasWithPolymorphic() throws Exception
    {
        String jsonStr = a2q("{'value': ['ab', {'nm' : 'Bob', 'A' : 17} ] }");
        
        PolyWrapperForAlias value = MAPPER.readValue(jsonStr, PolyWrapperForAlias.class);
        
        assertNotNull(value.value);
        AliasBean bean = (AliasBean) value.value;
        assertEquals("Bob", bean.name);
        assertEquals(17, bean._a);
    }

    @Test
    public void testGetMechanism()
    {
        final DeserializationConfig config = MAPPER.getDeserializationConfig();
        JavaType javaType = config.constructType(InnerSub4061B.class);
        List<NamedType> namedTypes = new ArrayList<>();
        namedTypes.add(new NamedType(InnerSub4061A.class));
        namedTypes.add(new NamedType(InnerSub4061B.class));
        
        SimpleNameIdResolver idResolver = SimpleNameIdResolver.construct(config, javaType, namedTypes, false, true);
        
        assertEquals(JsonTypeInfo.Id.SIMPLE_NAME, idResolver.getMechanism());
    }

    @Test
    public void testDuplicateNameLastOneWins() throws Exception
    {
        String jsonStr = a2q("{'@type':'DuplicateSubClass'}");
        
        // deser
        DuplicateSuperClass bean = MAPPER.readValue(jsonStr, DuplicateSuperClass.class);
        assertInstanceOf(com.fasterxml.jackson.databind.jsontype.DuplicateSubClass.class, bean);
    }
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.SIMPLE_NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicSub4061A.class),
        @JsonSubTypes.Type(value = BasicSub4061B.class)
})
class BasicSuper4061 { }

class BasicSub4061A extends BasicSuper4061 { }

class BasicSub4061B extends BasicSuper4061 { }

class MixedSub4061A extends JsonTypeInfoSimpleClassName4061Test.MixedSuper4061 { }

class MixedSub4061B extends JsonTypeInfoSimpleClassName4061Test.MixedSuper4061 { }

class MixedMinimalSub4061A extends JsonTypeInfoSimpleClassName4061Test.MixedMinimalSuper4061 { }

class MixedMinimalSub4061B extends JsonTypeInfoSimpleClassName4061Test.MixedMinimalSuper4061 { }

class DuplicateSubClass extends JsonTypeInfoSimpleClassName4061Test.DuplicateSuperClass { }
