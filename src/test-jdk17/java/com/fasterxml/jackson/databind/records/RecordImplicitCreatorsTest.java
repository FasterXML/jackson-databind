package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class RecordImplicitCreatorsTest extends DatabindTestUtil
{
    record RecordWithImplicitFactoryMethods(BigDecimal id, String name) {

        public static RecordWithImplicitFactoryMethods valueOf(int id) {
            return new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(id), "IntFactoryMethod");
        }

        public static RecordWithImplicitFactoryMethods valueOf(double id) {
            return new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(id), "DoubleFactoryMethod");
        }

        public static RecordWithImplicitFactoryMethods valueOf(String id) {
            return new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(Double.parseDouble(id)), "StringFactoryMethod");
        }
    }

    record RecordWithSingleValueConstructor(int id) {
    }

    record RecordWithSingleValueConstructorWithJsonValue(@JsonValue int id) {
    }

    record RecordWithSingleValueConstructorWithJsonValueAccessor(int id) {

        @JsonValue
        @Override
        public int id() {
            return id;
        }
    }

    record RecordWithNonCanonicalConstructor(int id, String name, String email) {

        public RecordWithNonCanonicalConstructor(int id, String email) {
            this(id, "NonCanonicalConstructor", email);
        }
    }

    /**
     * Similar to:
     * <pre>
     *   public class MyBean {
     *       ...
     *       // Single-arg constructor used by delegating creator.
     *       public MyBean(int id) { ... }
     *
     *       // No-arg constructor used by properties-based creator.
     *       public MyBean() {}
     *
     *       // Setters used by properties-based creator.
     *       public void setId(int id) { ... }
     *       public void setName(String name) { ... }
     *   }
     * </pre>
     */
    record RecordWithAltSingleValueConstructor(int id, String name) {

        public RecordWithAltSingleValueConstructor(int id) {
            this(id, "SingleValueConstructor");
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, implicit factory methods & "implicit" canonical constructor
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingImplicitIntegerFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods factoryMethodValue = MAPPER.readValue("123", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123), "IntFactoryMethod"), factoryMethodValue);
    }

    @Test
    public void testDeserializeUsingImplicitDoubleFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue("123.4", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "DoubleFactoryMethod"), value);
    }

    @Test
    public void testDeserializeUsingImplicitStringFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue("\"123.4\"", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "StringFactoryMethod"), value);
    }

    @Test
    public void testDeserializeUsingImplicitCanonicalConstructor_WhenImplicitFactoryMethodsExist() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue(
                "{\"id\":123.4,\"name\":\"CanonicalConstructor\"}",
                RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "CanonicalConstructor"), value);
    }

    @Test
    public void testDeserializeUsingImplicitFactoryMethod_WithAutoDetectCreatorsDisabled_WillFail() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .build();

        try {
            mapper.readValue("123", RecordWithImplicitFactoryMethods.class);

            fail("should not pass");
        } catch (DatabindException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithImplicitFactoryMethod");
            verifyException(e, "no int/Int-argument constructor/factory method");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, implicit single-value constructor
    /**********************************************************************
     */

    /**
     * This test-case is just for documentation purpose:
     * GOTCHA: For JavaBean, only having single-value constructor results in implicit delegating creator.  But for
     * Records, the CANONICAL single-value constructor results in properties-based creator.
     * <p/>
     * It will result in implicit delegating constructor only when:
     * <ul>
     *   <li>
     *     There's NON-CANONICAL single-value constructor - see
     *     {@link #testDeserializeUsingImplicitDelegatingConstructor()}, or
     *   </li>
     *   <li>
     *     {@code @JsonValue} annotation is used - see
     *     {@link #testDeserializeUsingImplicitSingleValueConstructor_WithJsonValue()},
     *     {@link #testDeserializeUsingImplicitSingleValueConstructor_WithJsonValueAccessor()}
     *   </li>
     * </ul>.
     * <p/>
     * yihtserns: maybe we can change this to adopt JavaBean's behaviour, but I prefer to not break existing behaviour
     * until and unless there's a discussion on this.
     */
    @Test
    public void testDeserializeUsingImplicitSingleValueConstructor() throws Exception {
        try {
            // Cannot use delegating creator, unlike when dealing with JavaBean
            MAPPER.readValue("123", RecordWithSingleValueConstructor.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithSingleValueConstructor");
            verifyException(e, "at least one Creator exists");
            verifyException(e, "no int/Int-argument constructor/factory method");
        }

        // Can only use properties-based creator
        RecordWithSingleValueConstructor value = MAPPER.readValue("{\"id\":123}", RecordWithSingleValueConstructor.class);
        assertEquals(new RecordWithSingleValueConstructor(123), value);
    }

    /**
     * This test-case is just for documentation purpose:
     * See {@link #testDeserializeUsingImplicitSingleValueConstructor}
     */
    @Test
    public void testDeserializeSingleValueConstructor_WithDelegatingConstructorDetector_WillFail() throws Exception {
        MAPPER.setConstructorDetector(ConstructorDetector.USE_DELEGATING);

        try {
            // Fail, no delegating creator
            MAPPER.readValue("123", RecordWithSingleValueConstructor.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithSingleValueConstructor");
            verifyException(e, "at least one Creator exists");
            verifyException(e, "no int/Int-argument constructor/factory method");
        }

        // Only have properties-based creator
        RecordWithSingleValueConstructor value = MAPPER.readValue("{\"id\":123}", RecordWithSingleValueConstructor.class);
        assertEquals(new RecordWithSingleValueConstructor(123), value);
    }

    /**
     * This is just to catch any potential regression.
     */
    @Test
    public void testDeserializeSingleValueConstructor_WithPropertiesBasedConstructorDetector_WillFail() throws Exception {
        MAPPER.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);

        try {
            // This should fail
            MAPPER.readValue("123", RecordWithSingleValueConstructor.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithSingleValueConstructor");
            verifyException(e, "at least one Creator exists");
            verifyException(e, "no int/Int-argument constructor/factory method");
        }

        // This should work
        RecordWithSingleValueConstructor value = MAPPER.readValue("{\"id\":123}", RecordWithSingleValueConstructor.class);
        assertEquals(new RecordWithSingleValueConstructor(123), value);
    }

    /*
    /**********************************************************************
    /* Test methods, implicit single-value constructor + @JsonValue
    /**********************************************************************
     */

    /**
     * [databind#3180]
     * This test-case is just for documentation purpose:
     * Unlike {@link #testDeserializeUsingImplicitSingleValueConstructor()}, annotating {@code @JsonValue}
     * to a Record's header results in a delegating constructor.
     */
    @Test
    public void testDeserializeUsingImplicitSingleValueConstructor_WithJsonValue() throws Exception {
        // Can use delegating creator
        RecordWithSingleValueConstructorWithJsonValue value = MAPPER.readValue(
                "123",
                RecordWithSingleValueConstructorWithJsonValue.class);
        assertEquals(new RecordWithSingleValueConstructorWithJsonValue(123), value);

        try {
            // Can no longer use properties-based creator
            MAPPER.readValue("{\"id\":123}", RecordWithSingleValueConstructorWithJsonValue.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithSingleValueConstructorWithJsonValue");
            verifyException(e, "although at least one Creator exists");
            verifyException(e, "cannot deserialize from Object value");
        }
    }

    /**
     * [databind#3180]
     * This test-case is just for documentation purpose:
     * Unlike {@link #testDeserializeUsingImplicitSingleValueConstructor()}, annotating {@code @JsonValue}
     * to the accessor results in a delegating creator.
     */
    @Test
    public void testDeserializeUsingImplicitSingleValueConstructor_WithJsonValueAccessor() throws Exception {
        // Can use delegating creator
        RecordWithSingleValueConstructorWithJsonValueAccessor value = MAPPER.readValue(
                "123",
                RecordWithSingleValueConstructorWithJsonValueAccessor.class);
        assertEquals(new RecordWithSingleValueConstructorWithJsonValueAccessor(123), value);

        try {
            // Can no longer use properties-based creator
            MAPPER.readValue("{\"id\":123}", RecordWithSingleValueConstructorWithJsonValueAccessor.class);

            fail("should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot construct instance");
            verifyException(e, "RecordWithSingleValueConstructorWithJsonValueAccessor");
            verifyException(e, "although at least one Creator exists");
            verifyException(e, "cannot deserialize from Object value");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, implicit properties-based + delegating constructor
    /**********************************************************************
     */

    @Test
    public void testDeserializeUsingImplicitPropertiesBasedConstructor() throws Exception {
        RecordWithAltSingleValueConstructor value = MAPPER.readValue(
                "{\"id\":123,\"name\":\"PropertiesBasedConstructor\"}",
                RecordWithAltSingleValueConstructor.class);

        assertEquals(new RecordWithAltSingleValueConstructor(123, "PropertiesBasedConstructor"), value);
    }

    /**
     * @see #testDeserializeUsingImplicitSingleValueConstructor()
     */
    @Test
    public void testDeserializeUsingImplicitDelegatingConstructor() throws Exception {
        RecordWithAltSingleValueConstructor value = MAPPER.readValue("123", RecordWithAltSingleValueConstructor.class);

        assertEquals(new RecordWithAltSingleValueConstructor(123, "SingleValueConstructor"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, implicit parameter names
    /**********************************************************************
     */

    @Test
    public void testDeserializeMultipleConstructorsRecord_WithImplicitParameterNames_WillUseCanonicalConstructor() throws Exception {
        MAPPER.setAnnotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector());

        RecordWithNonCanonicalConstructor value = MAPPER.readValue(
                "{\"id\":123,\"name\":\"Bob\",\"email\":\"bob@example.com\"}",
                RecordWithNonCanonicalConstructor.class);

        assertEquals(new RecordWithNonCanonicalConstructor(123, "Bob", "bob@example.com"), value);
    }

    @Test
    public void testDeserializeMultipleConstructorsRecord_WithImplicitParameterNames_WillIgnoreNonCanonicalConstructor() throws Exception {
        MAPPER.setAnnotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector());

        RecordWithNonCanonicalConstructor value = MAPPER.readValue(
                "{\"id\":123,\"email\":\"bob@example.com\"}",
                RecordWithNonCanonicalConstructor.class);

        assertEquals(new RecordWithNonCanonicalConstructor(123, null, "bob@example.com"), value);
    }
}
