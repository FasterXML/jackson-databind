package com.fasterxml.jackson.databind.deser.creators;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#562] Allow @JsonAnySetter on Creator constructors
public class AnySetterForCreator562Test extends DatabindTestUtil
{
    static class POJO562
    {
        String a;
        Map<String,Object> stuff;

        @JsonCreator
        public POJO562(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object> leftovers
        ) {
            this.a = a;
            stuff = leftovers;
        }
    }

    static class POJO562WithAnnotationOnBothCtorParamAndField
    {
        String a;
        @JsonAnySetter
        Map<String,Object> stuffFromField;
        Map<String,Object> stuffFromConstructor;

        @JsonCreator
        public POJO562WithAnnotationOnBothCtorParamAndField(@JsonProperty("a") String a,
                                                            @JsonAnySetter Map<String, Object> leftovers
        ) {
            this.a = a;
            stuffFromConstructor = leftovers;
        }
    }

    static class POJO562WithField
    {
        String a;
        Map<String,Object> stuff;

        public String b;

        @JsonCreator
        public POJO562WithField(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object> leftovers
        ) {
            this.a = a;
            stuff = leftovers;
        }
    }

    static class PojoWithNodeAnySetter
    {
        String a;
        JsonNode anySetterNode;

        @JsonCreator
        public PojoWithNodeAnySetter(@JsonProperty("a") String a,
            @JsonAnySetter JsonNode leftovers
        ) {
            this.a = a;
            anySetterNode = leftovers;
        }
    }

    static class MultipleAny562
    {
        @JsonCreator
        public MultipleAny562(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object> leftovers,
            @JsonAnySetter Map<String, Object> leftovers2) {
            throw new Error("Should never get here!");
        }
    }

    static class PojoWithDisabled
    {
        String a;
        Map<String,Object> stuff;

        @JsonCreator
        public PojoWithDisabled(@JsonProperty("a") String a,
            @JsonAnySetter(enabled = false) Map<String, Object> leftovers
        ) {
            this.a = a;
            stuff = leftovers;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void mapAnySetterViaCreator562() throws Exception
    {
        Map<String, Object> expected = new HashMap<>();
        expected.put("b", Integer.valueOf(42));
        expected.put("c", Integer.valueOf(111));

        POJO562 pojo = MAPPER.readValue(a2q(
                "{'a':'value', 'b':42, 'c': 111}"
                ),
                POJO562.class);

        assertEquals("value", pojo.a);
        assertEquals(expected, pojo.stuff);

        // Should be fine to vary ordering too
        pojo = MAPPER.readValue(a2q(
                "{'b':42, 'a':'value', 'c': 111}"
                ),
                POJO562.class);

        assertEquals("value", pojo.a);
        assertEquals(expected, pojo.stuff);

        // Should also initialize any-setter-Map even if no contents
        pojo = MAPPER.readValue(a2q("{'a':'value2'}"), POJO562.class);
        assertEquals("value2", pojo.a);
        assertEquals(new HashMap<>(), pojo.stuff);
    }

    // [databind#4634]
    @Test
    public void mapAnySetterViaCreatorWhenBothCreatorAndFieldAreAnnotated() throws Exception
    {
        Map<String, Object> expected = new HashMap<>();
        expected.put("b", Integer.valueOf(42));
        expected.put("c", Integer.valueOf(111));

        POJO562WithAnnotationOnBothCtorParamAndField pojo = MAPPER.readValue(a2q(
                "{'a':'value', 'b':42, 'c': 111}"
                ),
                POJO562WithAnnotationOnBothCtorParamAndField.class);

        assertEquals("value", pojo.a);
        assertEquals(expected, pojo.stuffFromConstructor);
        // In an ideal world, maybe exception should be thrown for annotating both field + constructor parameter,
        // but that scenario is possible in this imperfect world e.g. annotating `@JsonAnySetter` on a Record component
        // will cause that annotation to be (auto)propagated to both the field & constructor parameter (& accessor method)
        assertNull(pojo.stuffFromField);
    }

    // Creator and non-Creator props AND any-setter ought to be fine too
    @Test
    public void mapAnySetterViaCreatorAndField() throws Exception
    {
        POJO562WithField pojo = MAPPER.readValue(
                a2q("{'a':'value', 'b':'xyz', 'c': 'abc'}"),
                POJO562WithField.class);

        assertEquals("value", pojo.a);
        assertEquals("xyz", pojo.b);
        assertEquals(Collections.singletonMap("c", "abc"), pojo.stuff);
    }

    @Test
    public void testNodeAnySetterViaCreator562() throws Exception
    {
        PojoWithNodeAnySetter pojo = MAPPER.readValue(
                a2q("{'a':'value', 'b':42, 'c': 111}"),
                PojoWithNodeAnySetter.class);

        assertEquals("value", pojo.a);
        assertEquals(a2q("{'c':111,'b':42}"), pojo.anySetterNode + "");

        // Also ok to get nothing, resulting in empty ObjectNode
        pojo = MAPPER.readValue(a2q("{'a':'ok'}"), PojoWithNodeAnySetter.class);

        assertEquals("ok", pojo.a);
        assertEquals(MAPPER.createObjectNode(), pojo.anySetterNode);
    }

    @Test
    public void testAnyMapWithNullCreatorProp() throws Exception
    {
        ObjectMapper failOnNullMapper = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES).build();

        // 08-Jun-2024, tatu: Should be fine as we get empty Map for "no any setters"
        POJO562 value = failOnNullMapper.readValue(a2q("{'a':'value'}"), POJO562.class);
        assertEquals(Collections.emptyMap(), value.stuff);
    }

    @Test
    public void testAnyMapWithMissingCreatorProp() throws Exception
    {
        ObjectMapper failOnMissingMapper = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES).build();

        // Actually missing (no any props encountered)
        try {
            failOnMissingMapper.readValue(a2q("{'a':'value'}"), POJO562.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing creator property ''");
        }

        // But should NOT fail if we did find at least one...
        POJO562 value = failOnMissingMapper.readValue(a2q("{'a':'value','b':'x'}"), POJO562.class);
        assertEquals(Collections.singletonMap("b", "x"), value.stuff);
    }

    @Test
    public void testAnyMapWithNullOrMissingCreatorProp() throws Exception
    {
        ObjectMapper failOnBothMapper = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();
        try {
            failOnBothMapper.readValue(a2q("{'a':'value'}"), POJO562.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing creator property ''");
        }
    }

    @Test
    public void testAnySetterViaCreator562FailForDup() throws Exception
    {
        try {
            MAPPER.readValue("{}", MultipleAny562.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Invalid type definition");
            verifyException(e, "More than one 'any-setter'");
        }
    }

    @Test
    public void testAnySetterViaCreator562Disabled() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'a':'value', 'b':42, 'c': 111}"),
                PojoWithDisabled.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Invalid type definition for type");
            verifyException(e, "has no property name (and is not Injectable): can not use as property-based Creator");
        }
    }
}
