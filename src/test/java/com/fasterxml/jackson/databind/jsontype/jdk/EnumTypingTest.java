package com.fasterxml.jackson.databind.jsontype.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
public class EnumTypingTest extends BaseMapTest
{
    // note: As.WRAPPER_ARRAY worked initially; but as per [JACKSON-485], As.PROPERTY had issues
    @JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY)
    public interface EnumInterface { }

    public enum Tag implements EnumInterface
    { A, B };

    static class EnumInterfaceWrapper {
        public EnumInterface value;
    }

    static class EnumInterfaceList extends ArrayList<EnumInterface> { }

    static class TagList extends ArrayList<Tag> { }

    static enum TestEnum { A, B, C; }

    static class UntypedEnumBean
    {
       @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="__type")
        public Object value;

        public UntypedEnumBean() { }
        public UntypedEnumBean(TestEnum v) { value = v; }

        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="__type")
        public void setValue(Object o) {
            value = o;
        }
    }

    // for [databind#2605]
    static class EnumContaintingClass<ENUM_TYPE extends Enum<ENUM_TYPE>> {
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class"
        )
        public ENUM_TYPE selected;

        protected EnumContaintingClass() { }

        public EnumContaintingClass(ENUM_TYPE selected) {
          this.selected = selected;
        }
    }

    // [databind#2775]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME
// work-around:
//            , include = JsonTypeInfo.As.WRAPPER_ARRAY
            )
    @JsonSubTypes(@JsonSubTypes.Type(TestEnum2775.class))
    interface Base2775 {}

    @JsonTypeName("Test")
    enum TestEnum2775 implements Base2775 {
        VALUE;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testTagList() throws Exception
    {
        TagList list = new TagList();
        list.add(Tag.A);
        list.add(Tag.B);
        String json = MAPPER.writeValueAsString(list);

        TagList result = MAPPER.readValue(json, TagList.class);
        assertEquals(2, result.size());
        assertSame(Tag.A, result.get(0));
        assertSame(Tag.B, result.get(1));
    }

    public void testEnumInterface() throws Exception
    {
        String json = MAPPER.writeValueAsString(Tag.B);
        EnumInterface result = MAPPER.readValue(json, EnumInterface.class);
        assertSame(Tag.B, result);
    }

    public void testEnumInterfaceList() throws Exception
    {
        EnumInterfaceList list = new EnumInterfaceList();
        list.add(Tag.A);
        list.add(Tag.B);
        String json = MAPPER.writeValueAsString(list);

        EnumInterfaceList result = MAPPER.readValue(json, EnumInterfaceList.class);
        assertEquals(2, result.size());
        assertSame(Tag.A, result.get(0));
        assertSame(Tag.B, result.get(1));
    }

    public void testUntypedEnum() throws Exception
    {
        String str = MAPPER.writeValueAsString(new UntypedEnumBean(TestEnum.B));
        UntypedEnumBean result = MAPPER.readValue(str, UntypedEnumBean.class);
        assertNotNull(result);
        assertNotNull(result.value);
        Object ob = result.value;
        assertSame(TestEnum.class, ob.getClass());
        assertEquals(TestEnum.B, result.value);
    }

    // for [databind#2605]
    public void testRoundtrip() throws Exception
    {
        EnumContaintingClass<TestEnum> input = new EnumContaintingClass<TestEnum>(TestEnum.B);
        String json = MAPPER.writeValueAsString(input);
//      Object o = MAPPER.readerFor(EnumContaintingClass.class).readValue(json);
        Object o = MAPPER.readValue(json, EnumContaintingClass.class);
        assertNotNull(o);
    }

    // [databind#2775]
    public void testEnumAsSubtypeNoFailOnInvalidTypeId() throws Exception
    {
        final Base2775 testValue = TestEnum2775.VALUE;
        String json = MAPPER.writeValueAsString(testValue);
//System.err.println("JSON: "+json);

        Base2775 deserializedValue = MAPPER.readerFor(Base2775.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .readValue(json);
        assertEquals(testValue, deserializedValue);
    }
}
