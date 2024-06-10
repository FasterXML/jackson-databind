package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class RecordExplicitCreatorsTest extends DatabindTestUtil
{
    record RecordWithOneJsonPropertyWithoutJsonCreator(int id, String name) {

        public RecordWithOneJsonPropertyWithoutJsonCreator(@JsonProperty("id_only") int id) {
            this(id, "JsonPropertyConstructor");
        }

        public static RecordWithOneJsonPropertyWithoutJsonCreator valueOf(int id) {
            return new RecordWithOneJsonPropertyWithoutJsonCreator(id);
        }
    }

    record RecordWithTwoJsonPropertyWithoutJsonCreator(int id, String name, String email) {

        public RecordWithTwoJsonPropertyWithoutJsonCreator(@JsonProperty("the_id") int id, @JsonProperty("the_email") String email) {
            this(id, "TwoJsonPropertyConstructor", email);
        }

        public static RecordWithTwoJsonPropertyWithoutJsonCreator valueOf(int id) {
            return new RecordWithTwoJsonPropertyWithoutJsonCreator(id, "factory@example.com");
        }
    }

    record RecordWithJsonPropertyAndImplicitPropertyWithoutJsonCreator(int id, String name, String email) {

        public RecordWithJsonPropertyAndImplicitPropertyWithoutJsonCreator(@JsonProperty("id_only") int id, String email) {
            this(id, "JsonPropertyConstructor", email);
        }
    }

    record RecordWithJsonPropertyWithJsonCreator(int id, String name) {

        @JsonCreator
        public RecordWithJsonPropertyWithJsonCreator(@JsonProperty("id_only") int id) {
            this(id, "JsonCreatorConstructor");
        }

        public static RecordWithJsonPropertyWithJsonCreator valueOf(int id) {
            return new RecordWithJsonPropertyWithJsonCreator(id);
        }
    }

    record RecordWithJsonPropertyAndImplicitPropertyWithJsonCreator(int id, String name, String email) {

        @JsonCreator
        public RecordWithJsonPropertyAndImplicitPropertyWithJsonCreator(@JsonProperty("id_only") int id, String email) {
            this(id, "JsonPropertyConstructor", email);
        }
    }

    record RecordWithMultiExplicitDelegatingConstructor(int id, String name) {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public RecordWithMultiExplicitDelegatingConstructor(int id) {
            this(id, "IntConstructor");
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public RecordWithMultiExplicitDelegatingConstructor(String id) {
            this(Integer.parseInt(id), "StringConstructor");
        }
    }

    record RecordWithDisabledJsonCreator(int id, String name) {

        @JsonCreator(mode = JsonCreator.Mode.DISABLED)
        RecordWithDisabledJsonCreator {
        }
    }

    record RecordWithExplicitFactoryMethod(BigDecimal id, String name) {

        @JsonCreator
        public static RecordWithExplicitFactoryMethod valueOf(int id) {
            return new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(id), "IntFactoryMethod");
        }

        public static RecordWithExplicitFactoryMethod valueOf(double id) {
            return new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(id), "DoubleFactoryMethod");
        }

        @JsonCreator
        public static RecordWithExplicitFactoryMethod valueOf(String id) {
            return new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(Double.parseDouble(id)), "StringFactoryMethod");
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS) // So that test cases don't have to assert weird error messages
            .build();

    /*
    /**********************************************************************
    /* Test methods, "implicit" (?) constructor with JsonProperty without JsonCreator
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingJsonPropertyConstructor_WithoutJsonCreator() throws Exception {
        RecordWithOneJsonPropertyWithoutJsonCreator oneJsonPropertyValue = MAPPER.readValue(
                "{\"id_only\":123}",
                RecordWithOneJsonPropertyWithoutJsonCreator.class);
        assertEquals(new RecordWithOneJsonPropertyWithoutJsonCreator(123), oneJsonPropertyValue);

        RecordWithTwoJsonPropertyWithoutJsonCreator twoJsonPropertyValue = MAPPER.readValue(
                "{\"the_id\":123,\"the_email\":\"bob@example.com\"}",
                RecordWithTwoJsonPropertyWithoutJsonCreator.class);
        assertEquals(new RecordWithTwoJsonPropertyWithoutJsonCreator(123, "bob@example.com"), twoJsonPropertyValue);
    }

    /**
     * Only 1 properties-based creator allowed, so can no longer use the (un-annotated) canonical constructor.
     */
    @Test
    public void testDeserializeUsingCanonicalConstructor_WhenJsonPropertyConstructorExists_WillFail() throws Exception {
        try {
            MAPPER.readValue(
                    "{\"id\":123,\"name\":\"Bobby\"}",
                    RecordWithOneJsonPropertyWithoutJsonCreator.class);

            fail("should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field \"id\"");
            verifyException(e, "RecordWithOneJsonPropertyWithoutJsonCreator");
            verifyException(e, "one known property: \"id_only\"");
        }

        try {
            MAPPER.readValue(
                    "{\"id\":123,\"name\":\"Bobby\",\"email\":\"bobby@example.com\"}",
                    RecordWithTwoJsonPropertyWithoutJsonCreator.class);

            fail("should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field \"id\"");
            verifyException(e, "RecordWithTwoJsonPropertyWithoutJsonCreator");
            verifyException(e, "2 known properties: \"the_id\", \"the_email\"");
        }
    }

    @Test
    public void testDeserializeUsingImplicitFactoryMethod_WhenJsonPropertyConstructorExists() throws Exception {
        RecordWithOneJsonPropertyWithoutJsonCreator oneJsonPropertyValue = MAPPER.readValue(
                "123",
                RecordWithOneJsonPropertyWithoutJsonCreator.class);
        assertEquals(RecordWithOneJsonPropertyWithoutJsonCreator.valueOf(123), oneJsonPropertyValue);

        RecordWithTwoJsonPropertyWithoutJsonCreator twoJsonPropertyValue = MAPPER.readValue(
                "123",
                RecordWithTwoJsonPropertyWithoutJsonCreator.class);
        assertEquals(RecordWithTwoJsonPropertyWithoutJsonCreator.valueOf(123), twoJsonPropertyValue);
    }

    /*
    /**********************************************************************
    /* Test methods, explicit constructor with JsonProperty with JsonCreator
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingJsonCreatorConstructor() throws Exception {
        RecordWithJsonPropertyWithJsonCreator value = MAPPER.readValue("{\"id_only\":123}", RecordWithJsonPropertyWithJsonCreator.class);

        assertEquals(new RecordWithJsonPropertyWithJsonCreator(123), value);
    }

    /**
     * Only 1 properties-based creator allowed, so can no longer use the (un-annotated) canonical constructor
     */
    @Test
    public void testDeserializeUsingCanonicalConstructor_WhenJsonCreatorConstructorExists_WillFail() throws Exception {
        try {
            MAPPER.readValue("{\"id\":123,\"name\":\"Bobby\"}", RecordWithJsonPropertyWithJsonCreator.class);

            fail("should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field \"id\"");
            verifyException(e, "RecordWithJsonPropertyWithJsonCreator");
            verifyException(e, "one known property: \"id_only\"");
        }
    }

    // 23-May-2024, tatu: Logic changed as part of [databind#4515]: explicit properties-based
    //   Creator does NOT block implicit delegating Creators. So formerly (pre-2.18) failing
    //   case is now expected to pass.
    @Test
    public void testDeserializeUsingImplicitFactoryMethod_WhenJsonCreatorConstructorExists_WillFail() throws Exception {
        RecordWithJsonPropertyWithJsonCreator value = MAPPER.readValue("123",
                RecordWithJsonPropertyWithJsonCreator.class);
        assertEquals(123, value.id());
        assertEquals("JsonCreatorConstructor", value.name());
    }

    /*
    /**********************************************************************
    /* Test methods, multiple explicit delegating constructors
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingExplicitDelegatingConstructors() throws Exception {
        RecordWithMultiExplicitDelegatingConstructor intConstructorValue = MAPPER.readValue("123", RecordWithMultiExplicitDelegatingConstructor.class);
        assertEquals(new RecordWithMultiExplicitDelegatingConstructor(123, "IntConstructor"), intConstructorValue);

        RecordWithMultiExplicitDelegatingConstructor stringConstructorValue = MAPPER.readValue("\"123\"", RecordWithMultiExplicitDelegatingConstructor.class);
        assertEquals(new RecordWithMultiExplicitDelegatingConstructor(123, "StringConstructor"), stringConstructorValue);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonCreator.mode=DISABLED
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingDisabledConstructors_WillFail() throws Exception {
        try {
            MAPPER.readValue("{\"id\":123,\"name\":\"Bobby\"}",
                    RecordWithDisabledJsonCreator.class);
            fail("should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "RecordWithDisabledJsonCreator");
            verifyException(e, "Cannot construct instance");
            verifyException(e, "no Creators, like default constructor, exist");
            verifyException(e, "cannot deserialize from Object value");
        }

    }

    /*
    /**********************************************************************
    /* Test methods, explicit factory methods
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingExplicitFactoryMethods() throws Exception {
        RecordWithExplicitFactoryMethod intFactoryValue = MAPPER.readValue("123", RecordWithExplicitFactoryMethod.class);
        assertEquals(new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(123), "IntFactoryMethod"), intFactoryValue);

        RecordWithExplicitFactoryMethod stringFactoryValue = MAPPER.readValue("\"123.4\"", RecordWithExplicitFactoryMethod.class);
        assertEquals(new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(123.4), "StringFactoryMethod"), stringFactoryValue);
    }

    /**
     * Implicit factory methods detection is only activated when there's no explicit (i.e. annotated
     * with {@link JsonCreator}) factory methods.
     */
    @Test
    public void testDeserializeUsingImplicitFactoryMethods_WhenExplicitFactoryMethodsExist_WillFail() throws Exception {
        try {
            MAPPER.readValue("123.4", RecordWithExplicitFactoryMethod.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithExplicitFactoryMethod");
            verifyException(e, "although at least one Creator exists");
            verifyException(e, "no double/Double-argument constructor/factory");
        }
    }

    /**
     * Just like how no-arg constructor + setters will still be used to deserialize JSON Object even when
     * there's JsonCreator factory method(s) in the JavaBean class.
     */
    @Test
    public void testDeserializeUsingImplicitCanonicalConstructor_WhenFactoryMethodsExist() throws Exception {
        RecordWithExplicitFactoryMethod value = MAPPER.readValue("{\"id\":123.4,\"name\":\"CanonicalConstructor\"}", RecordWithExplicitFactoryMethod.class);

        assertEquals(new RecordWithExplicitFactoryMethod(BigDecimal.valueOf(123.4), "CanonicalConstructor"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, implicit parameter names
    /**********************************************************************
     */

    @Test
    public void testDeserializeMultipleConstructorsRecord_WithExplicitAndImplicitParameterNames_WithJsonCreator() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                .build();

        RecordWithJsonPropertyAndImplicitPropertyWithJsonCreator value = mapper.readValue(
                "{\"id_only\":123,\"email\":\"bob@example.com\"}",
                RecordWithJsonPropertyAndImplicitPropertyWithJsonCreator.class);
        assertEquals(new RecordWithJsonPropertyAndImplicitPropertyWithJsonCreator(123, "bob@example.com"), value);
    }

    /**
     * This test used to fail before 2.18; but with Bean Property introspection
     * rewrite now works!
     *
     * @see #testDeserializeUsingJsonCreatorConstructor()
     * @see #testDeserializeUsingCanonicalConstructor_WhenJsonCreatorConstructorExists_WillFail()
     */
    @Test
    public void testDeserializeMultipleConstructorsRecord_WithExplicitAndImplicitParameterNames() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                .build();
        RecordWithJsonPropertyAndImplicitPropertyWithoutJsonCreator value = mapper.readValue(
                    "{\"id_only\":123,\"email\":\"bob@example.com\"}",
                    RecordWithJsonPropertyAndImplicitPropertyWithoutJsonCreator.class);
        assertEquals(123, value.id);
        assertEquals("bob@example.com", value.email);
    }
}
