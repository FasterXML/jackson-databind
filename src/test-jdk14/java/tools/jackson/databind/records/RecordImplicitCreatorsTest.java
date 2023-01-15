package tools.jackson.databind.records;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.exc.MismatchedInputException;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

public class RecordImplicitCreatorsTest extends BaseMapTest
{
    public record RecordWithImplicitFactoryMethods(BigDecimal id, String name) {

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

    public record RecordWithSingleValueConstructor(int id) {
    }

    public record RecordWithNonCanonicalConstructor(int id, String name, String email) {

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
    public record RecordWithAltSingleValueConstructor(int id, String name) {

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

    public void testDeserializeUsingImplicitIntegerFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods factoryMethodValue = MAPPER.readValue("123", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123), "IntFactoryMethod"), factoryMethodValue);
    }

    public void testDeserializeUsingImplicitDoubleFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue("123.4", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "DoubleFactoryMethod"), value);
    }

    public void testDeserializeUsingImplicitStringFactoryMethod() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue("\"123.4\"", RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "StringFactoryMethod"), value);
    }

    public void testDeserializeUsingImplicitCanonicalConstructor_WhenImplicitFactoryMethodsExist() throws Exception {
        RecordWithImplicitFactoryMethods value = MAPPER.readValue(
                "{\"id\":123.4,\"name\":\"CanonicalConstructor\"}",
                RecordWithImplicitFactoryMethods.class);

        assertEquals(new RecordWithImplicitFactoryMethods(BigDecimal.valueOf(123.4), "CanonicalConstructor"), value);
    }

    public void testDeserializeUsingImplicitFactoryMethod_WithAutoDetectCreatorsDisabled_WillFail() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc.withScalarConstructorVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
                .build();
        // was: MAPPER.disable(MapperFeature.AUTO_DETECT_CREATORS);

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
     * Only when there's NON-CANONICAL single-value constructor will there be implicit delegating creator - see
     * {@link #testDeserializeUsingImplicitDelegatingConstructor()}.
     * <p/>
     * yihtserns: maybe we can change this to adopt JavaBean's behaviour, but I prefer to not break existing behaviour
     * until and unless there's a discussion on this.
     */
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
    public void testDeserializeSingleValueConstructor_WithDelegatingConstructorDetector_WillFail() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .constructorDetector(ConstructorDetector.USE_DELEGATING)
                .build();

        try {
            // Fail, no delegating creator
            mapper.readValue("123", RecordWithSingleValueConstructor.class);

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
    public void testDeserializeSingleValueConstructor_WithPropertiesBasedConstructorDetector_WillFail() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build();
        try {
            // This should fail
            mapper.readValue("123", RecordWithSingleValueConstructor.class);

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
    /* Test methods, implicit properties-based + delegating constructor
    /**********************************************************************
     */

    public void testDeserializeUsingImplicitPropertiesBasedConstructor() throws Exception {
        RecordWithAltSingleValueConstructor value = MAPPER.readValue(
                "{\"id\":123,\"name\":\"PropertiesBasedConstructor\"}",
                RecordWithAltSingleValueConstructor.class);

        assertEquals(new RecordWithAltSingleValueConstructor(123, "PropertiesBasedConstructor"), value);
    }

    /**
     * @see #testDeserializeUsingImplicitSingleValueConstructor()
     */
    public void testDeserializeUsingImplicitDelegatingConstructor() throws Exception {
        RecordWithAltSingleValueConstructor value = MAPPER.readValue("123", RecordWithAltSingleValueConstructor.class);

        assertEquals(new RecordWithAltSingleValueConstructor(123, "SingleValueConstructor"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, implicit parameter names
    /**********************************************************************
     */

    public void testDeserializeMultipleConstructorsRecord_WithImplicitParameterNames_WillUseCanonicalConstructor() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                .build();

        RecordWithNonCanonicalConstructor value = mapper.readValue(
                "{\"id\":123,\"name\":\"Bob\",\"email\":\"bob@example.com\"}",
                RecordWithNonCanonicalConstructor.class);

        assertEquals(new RecordWithNonCanonicalConstructor(123, "Bob", "bob@example.com"), value);
    }

    public void testDeserializeMultipleConstructorsRecord_WithImplicitParameterNames_WillIgnoreNonCanonicalConstructor() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new Jdk8ConstructorParameterNameAnnotationIntrospector())
                .build();
        RecordWithNonCanonicalConstructor value = mapper.readValue(
                "{\"id\":123,\"email\":\"bob@example.com\"}",
                RecordWithNonCanonicalConstructor.class);

        assertEquals(new RecordWithNonCanonicalConstructor(123, null, "bob@example.com"), value);
    }
}
