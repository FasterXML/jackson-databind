package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test for <a href="https://github.com/FasterXML/jackson-databind/issues/4061">
 * [databind#4061] Add JsonTypeInfo.Id.SIMPLE_NAME using Class::getSimpleName</a>
 */

public class JsonTypeInfoSimpleClassName4061Test extends BaseMapTest
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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = newJsonMapper();

    // inner class that has contains dollar sign
    public void testInnerClass() throws Exception
    {
        // ser
        assertEquals(
                a2q("{'@simpl':'InnerSub4061A'}"),
                MAPPER.writeValueAsString(new InnerSub4061A()));

        // deser <- breaks!
        InnerSuper4061 bean = MAPPER.readValue(a2q("{'@simpl':'InnerSub4061B'}"), InnerSuper4061.class);
        assertType(bean, InnerSuper4061.class);
    }

    // inner class that has contains dollar sign
    public void testMinimalInnerClass() throws Exception
    {
        // ser
        assertEquals(
                a2q("{'@c':'.JsonTypeInfoSimpleClassName4061Test$MinimalInnerSub4061A'}"),
                MAPPER.writeValueAsString(new MinimalInnerSub4061A()));

        // deser <- breaks!
        MinimalInnerSuper4061 bean = MAPPER.readValue(a2q("{'@c':'.JsonTypeInfoSimpleClassName4061Test$MinimalInnerSub4061A'}"), MinimalInnerSuper4061.class);
        assertType(bean, MinimalInnerSuper4061.class);
    }

    // Basic : non-inner class, without dollar sign
    public void testBasicClass() throws Exception
    {
        // ser
        assertEquals(
                a2q("{'@simpl':'BasicSub4061A'}"),
                MAPPER.writeValueAsString(new BasicSub4061A()));

        // deser
        BasicSuper4061 bean = MAPPER.readValue(a2q("{'@simpl':'BasicSub4061B'}"), BasicSuper4061.class);
        assertType(bean, BasicSuper4061.class);
        assertType(bean, BasicSub4061B.class);
    }
    
    // Mixed SimpleClassName : parent as inner, subtype as basic
    public void testMixedClass() throws Exception
    {
        // ser
        assertEquals(
                a2q("{'@simpl':'MixedSub4061A'}"),
                MAPPER.writeValueAsString(new MixedSub4061A()));

        // deser
        MixedSuper4061 bean = MAPPER.readValue(a2q("{'@simpl':'MixedSub4061B'}"), MixedSuper4061.class);
        assertType(bean, MixedSuper4061.class);
        assertType(bean, MixedSub4061B.class);
    }
    
    // Mixed MinimalClass : parent as inner, subtype as basic
    public void testMixedMinimalClass() throws Exception
    {
        // ser
        assertEquals(
                a2q("{'@c':'.MixedMinimalSub4061A'}"),
                MAPPER.writeValueAsString(new MixedMinimalSub4061A()));

        // deser
        MixedMinimalSuper4061 bean = MAPPER.readValue(a2q("{'@c':'.MixedMinimalSub4061B'}"), MixedMinimalSuper4061.class);
        assertType(bean, MixedMinimalSuper4061.class);
        assertType(bean, MixedMinimalSub4061B.class);
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
