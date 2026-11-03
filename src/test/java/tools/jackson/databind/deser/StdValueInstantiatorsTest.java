package tools.jackson.databind.deser;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#2978]
public class StdValueInstantiatorsTest
    extends DatabindTestUtil
{
    static class A {
        final int creatorType;
        
        A(int value) {
            this.creatorType = 1;
        }
        
        A(long value) {
            this.creatorType = 2;
        }
        
        A(BigInteger value) {
            this.creatorType = 3;
        }
        
        A(double value) {
            this.creatorType = 4;
        }
    }

    static class B {
        final int creatorType;
        
        B(long value) {
            this.creatorType = 2;
        }
        
        B(BigInteger value) {
            this.creatorType = 3;
        }
        
        B(double value) {
            this.creatorType = 4;
        }
    }

    static class C {
        final int creatorType;
        
        C(BigInteger value) {
            this.creatorType = 3;
        }
        
        C(double value) {
            this.creatorType = 4;
        }
    }

    static final class D {
        
        D(double value) {
            throw new IllegalArgumentException("boo");
        }
    }

    static class A2 {
        final int creatorType;
        
        A2(int value) {
            this.creatorType = 1;
        }
        
        A2(long value) {
            this.creatorType = 2;
        }
        
        A2(BigInteger value) {
            this.creatorType = 3;
        }
        
        A2(double value) {
            this.creatorType = 4;
        }
    }

    static class B2 {
        final int creatorType;
        
        B2(BigInteger value) {
            this.creatorType = 3;
        }
        
        B2(double value) {
            this.creatorType = 4;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private static final long LONG_TEST_VALUE = 12345678901L;

    static class Stuff {
        final double value;
        
        Stuff(double value) {
            this.value = value;
        }
    }

    @Test
    public void testJsonIntegerToDouble() throws Exception {
        Stuff a = MAPPER.readValue("5", Stuff.class);
        assertEquals(5, a.value);
    }
    
    @Test
    public void testJsonLongToDouble() throws Exception {
        assertTrue(LONG_TEST_VALUE > Integer.MAX_VALUE);
        Stuff a = MAPPER.readValue(String.valueOf(LONG_TEST_VALUE), Stuff.class);
        assertEquals(LONG_TEST_VALUE, a.value);
    }
    
    @Test
    public void testJsonIntegerDeserializationPrefersInt() throws Exception {
        A a = MAPPER.readValue("5", A.class);
        assertEquals(1, a.creatorType);
    }
    
    @Test
    public void testJsonIntegerDeserializationPrefersLong() throws Exception {
        B a = MAPPER.readValue("5", B.class);
        assertEquals(2, a.creatorType);
    }
    
    @Test
    public void testJsonIntegerDeserializationPrefersBigInteger() throws Exception {
        C a = MAPPER.readValue("5", C.class);
        assertEquals(3, a.creatorType);
    }
    
    @Test
    public void testJsonLongDeserializationPrefersLong() throws Exception {
        A2 a = MAPPER.readValue(String.valueOf(LONG_TEST_VALUE), A2.class);
        assertEquals(2, a.creatorType);
    }
    
    @Test
    public void testJsonLongDeserializationPrefersBigInteger() throws Exception {
        B2 a = MAPPER.readValue(String.valueOf(LONG_TEST_VALUE), B2.class);
        assertEquals(3, a.creatorType);
    }

    @Test
    public void testJsonIntegerIntoDoubleConstructorThrows() throws Exception {
        try {
            MAPPER.readValue("5", D.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
            assertEquals("boo", e.getCause().getMessage());
        }
    }

    @Test
    public void testJsonLongIntoDoubleConstructorThrows() throws Exception {
        try {
            MAPPER.readValue(String.valueOf(LONG_TEST_VALUE), D.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
            assertEquals("boo", e.getCause().getMessage());
        }
    }
}
