package com.fasterxml.jackson.databind.deser.std;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#2978]
public class StdValueInstantiatorTest
{
    private static final long LONG_TEST_VALUE = 12345678901L;
    
    @Test
    public void testDoubleValidation_valid() {
        assertEquals(0d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ZERO));
        assertEquals(1d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ONE));
        assertEquals(10d, StdValueInstantiator.tryConvertToDouble(BigDecimal.TEN));
        assertEquals(-1.5d, StdValueInstantiator.tryConvertToDouble(BigDecimal.valueOf(-1.5d)));
    }

    @Test
    public void testDoubleValidation_invalid() {
        BigDecimal value = BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE));
        assertNull(StdValueInstantiator.tryConvertToDouble(value));
    }
    
    @Test
    public void testJsonIntegerToDouble() throws Exception {
        ObjectMapper m = new ObjectMapper();
        Stuff a = m.readValue("5", Stuff.class);
        assertEquals(5, a.value);
    }
    
    @Test
    public void testJsonLongToDouble() throws Exception {
        ObjectMapper m = new ObjectMapper();
        assertTrue(LONG_TEST_VALUE > Integer.MAX_VALUE);
        Stuff a = m.readValue(String.valueOf(LONG_TEST_VALUE), Stuff.class);
        assertEquals(LONG_TEST_VALUE, a.value);
    }
    
    static class Stuff {
        final double value;
        
        Stuff(double value) {
            this.value = value;
        }
    }
    
    @Test
    public void testJsonIntegerDeserializationPrefersInt() throws Exception {
        ObjectMapper m = new ObjectMapper();
        A a = m.readValue("5", A.class);
        assertEquals(1, a.creatorType);
    }
    
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
    
    @Test
    public void testJsonIntegerDeserializationPrefersLong() throws Exception {
        ObjectMapper m = new ObjectMapper();
        B a = m.readValue("5", B.class);
        assertEquals(2, a.creatorType);
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
    
    @Test
    public void testJsonIntegerDeserializationPrefersBigInteger() throws Exception {
        ObjectMapper m = new ObjectMapper();
        C a = m.readValue("5", C.class);
        assertEquals(3, a.creatorType);
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
    
    @Test
    public void testJsonLongDeserializationPrefersLong() throws Exception {
        ObjectMapper m = new ObjectMapper();
        A2 a = m.readValue(String.valueOf(LONG_TEST_VALUE), A2.class);
        assertEquals(2, a.creatorType);
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
    
    @Test
    public void testJsonLongDeserializationPrefersBigInteger() throws Exception {
        ObjectMapper m = new ObjectMapper();
        B2 a = m.readValue(String.valueOf(LONG_TEST_VALUE), B2.class);
        assertEquals(3, a.creatorType);
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
    
    @Test
    public void testJsonIntegerIntoDoubleConstructorThrows() throws Exception {
        ObjectMapper m = new ObjectMapper();
        try {
            m.readValue("5", D.class);
            fail();
        } catch (ValueInstantiationException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals("boo", e.getCause().getMessage());
        }
    }
    
    static final class D {
        
        D(double value) {
            throw new IllegalArgumentException("boo");
        }
    }
    
    @Test
    public void testJsonLongIntoDoubleConstructorThrows() throws Exception {
        ObjectMapper m = new ObjectMapper();
        try {
            m.readValue(String.valueOf(LONG_TEST_VALUE), D.class);
            fail();
        } catch (ValueInstantiationException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals("boo", e.getCause().getMessage());
        }
    }
    
}
