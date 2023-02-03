package com.fasterxml.jackson.databind.jsontype.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying that types that serialize as JSON Arrays
 * get properly serialized with types (esp. for contents, and
 * gracefully handling Lists themselves too)
 */
public class TypedArraySerTest
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Let's claim we need type here too (although we won't
     * really use any sub-classes)
     */
    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
    static class TypedList<T> extends ArrayList<T> { }

    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
    static class TypedListAsProp<T> extends ArrayList<T> { }

    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    static class TypedListAsWrapper<T> extends LinkedList<T> { }

    // Mix-in to force wrapper for things like primitive arrays
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    interface WrapperMixIn { }

    // for [JACKSON-341]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({ @JsonSubTypes.Type(B.class) })
    interface A { }

    @JsonTypeName("BB")
    static class B implements A {
        public int value = 2;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY)
    @JsonTypeName("bean")
    static class Bean {
        public int x = 0;
    }

    static class BeanListWrapper {
        @JsonView({Object.class})
        public List<Bean> beans = new ArrayList<Bean>();
        {
            beans.add(new Bean());
        }
    }

    /*
    /**********************************************************
    /* Unit tests, Lists
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testListWithPolymorphic() throws Exception
    {
        BeanListWrapper beans = new BeanListWrapper();
        assertEquals("{\"beans\":[{\"@type\":\"bean\",\"x\":0}]}", MAPPER.writeValueAsString(beans));
        // Related to [JACKSON-364]
        ObjectWriter w = MAPPER.writerWithView(Object.class);
        assertEquals("{\"beans\":[{\"@type\":\"bean\",\"x\":0}]}", w.writeValueAsString(beans));
    }

    public void testIntList() throws Exception
    {
        TypedList<Integer> input = new TypedList<Integer>();
        input.add(5);
        input.add(13);
        // uses WRAPPER_ARRAY inclusion:
        assertEquals("[\""+TypedList.class.getName()+"\",[5,13]]",
                MAPPER.writeValueAsString(input));
    }

    // Similar to above, but this time let's request adding type info
    // as property. That would not work (since there's no JSON Object to
    // add property in), so it should revert to method used with
    // ARRAY_WRAPPER method.
    public void testStringListAsProp() throws Exception
    {
        TypedListAsProp<String> input = new TypedListAsProp<String>();
        input.add("a");
        input.add("b");
        assertEquals("[\""+TypedListAsProp.class.getName()+"\",[\"a\",\"b\"]]",
                MAPPER.writeValueAsString(input));
    }

    public void testStringListAsObjectWrapper() throws Exception
    {
        TypedListAsWrapper<Boolean> input = new TypedListAsWrapper<Boolean>();
        input.add(true);
        input.add(null);
        input.add(false);
        // Can wrap in JSON Object for wrapped style... also, will use
        // non-qualified class name as type name, since there are no
        // annotations
        String expName = "TypedArraySerTest$TypedListAsWrapper";
        assertEquals("{\""+expName+"\":[true,null,false]}",
                MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************
    /* Unit tests, primitive arrays
    /**********************************************************
     */

    public void testIntArray() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(int[].class, WrapperMixIn.class);
        int[] input = new int[] { 1, 2, 3 };
        String clsName = int[].class.getName();
        assertEquals("{\""+clsName+"\":[1,2,3]}", m.writeValueAsString(input));
    }

    /*
    /**********************************************************
    /* Unit tests, generic arrays
    /**********************************************************
     */

    public void testGenericArray() throws Exception
    {
        final A[] input = new A[] { new B() };
        final String EXP = "[{\"BB\":{\"value\":2}}]";

        // first, with defaults
        assertEquals(EXP, MAPPER.writeValueAsString(input));

        // then with static typing enabled:
        ObjectMapper m = jsonMapperBuilder()
                .configure(MapperFeature.USE_STATIC_TYPING, true)
                .build();
        assertEquals(EXP, m.writeValueAsString(input));
    }
}
