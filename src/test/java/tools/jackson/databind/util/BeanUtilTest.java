package tools.jackson.databind.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BeanUtilTest extends DatabindTestUtil
{
    @Test
    public void testGetDefaultValue()
    {
        TypeFactory tf = defaultTypeFactory();
        // For collection/array/Map types, should give `NOT_EMPTY`:
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(Map.class)));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(List.class)));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(Object[].class)));
        // as well as ReferenceTypes, String
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(AtomicReference.class)));
        assertEquals("",
                BeanUtil.getDefaultValue(tf.constructType(String.class)));
        // primitive/wrappers have others
        assertEquals(Integer.valueOf(0),
                BeanUtil.getDefaultValue(tf.constructType(Integer.class)));


        // but POJOs have no real default
        assertNull(BeanUtil.getDefaultValue(tf.constructType(getClass())));
    }

    @Test
    public void testGetDefaultValueForDate()
    {
        TypeFactory tf = defaultTypeFactory();
        Object result = BeanUtil.getDefaultValue(tf.constructType(Date.class));
        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(0L, ((Date) result).getTime());
    }

    @Test
    public void testGetDefaultValueForCalendar()
    {
        TypeFactory tf = defaultTypeFactory();
        Object result = BeanUtil.getDefaultValue(tf.constructType(Calendar.class));
        assertNotNull(result);
        assertTrue(result instanceof Calendar);
        assertEquals(0L, ((Calendar) result).getTimeInMillis());
    }

    @Test
    public void testGetDefaultValueForGregorianCalendar()
    {
        TypeFactory tf = defaultTypeFactory();
        Object result = BeanUtil.getDefaultValue(tf.constructType(GregorianCalendar.class));
        assertNotNull(result);
        assertTrue(result instanceof Calendar);
        assertEquals(0L, ((Calendar) result).getTimeInMillis());
    }

    @Deprecated
    @Test
    public void testDeprecatedStdManglePropertyName()
    {
        // Empty name after offset
        assertNull(BeanUtil.stdManglePropertyName("get", 3));

        // Starts with lowercase - return as-is
        assertEquals("value", BeanUtil.stdManglePropertyName("getValue", 3));

        // Single uppercase letter - should lowercase
        assertEquals("x", BeanUtil.stdManglePropertyName("getX", 3));

        // Two consecutive uppercase letters - keep as-is (Java Beans spec)
        assertEquals("URL", BeanUtil.stdManglePropertyName("getURL", 3));

        // Standard property name
        assertEquals("name", BeanUtil.stdManglePropertyName("getName", 3));

        // Property starting with uppercase, second lowercase - should lowercase first
        assertEquals("value", BeanUtil.stdManglePropertyName("Value", 0));
    }

    @Test
    public void testCheckUnsupportedTypeForSupportedType()
    {
        TypeFactory tf = defaultTypeFactory();
        // Regular types should return null
        assertNull(BeanUtil.checkUnsupportedType(null, tf.constructType(String.class)));
        assertNull(BeanUtil.checkUnsupportedType(null, tf.constructType(Integer.class)));
        assertNull(BeanUtil.checkUnsupportedType(null, tf.constructType(List.class)));
    }

    @Test
    public void testIsJodaTimeClass()
    {
        // Test with non-Joda Time classes
        assertFalse(BeanUtil.isJodaTimeClass(String.class));
        assertFalse(BeanUtil.isJodaTimeClass(Date.class));
        assertFalse(BeanUtil.isJodaTimeClass(Calendar.class));
    }
}
