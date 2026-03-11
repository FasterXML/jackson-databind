package tools.jackson.databind.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

public class BeanUtilTest extends DatabindTestUtil
{
    @Test
    public void testGetDefaultValueGeneral()
    {
        TypeFactory tf = defaultTypeFactory();
        // For collection/array/Map types, should give `NOT_EMPTY`:
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.propertyDefaultValue(tf.constructType(Map.class), true));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.propertyDefaultValue(tf.constructType(List.class), true));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.propertyDefaultValue(tf.constructType(Object[].class), true));
        // as well as ReferenceTypes, String
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.propertyDefaultValue(tf.constructType(AtomicReference.class), true));
        assertEquals("",
                BeanUtil.propertyDefaultValue(tf.constructType(String.class), true));

        // primitives have specific defaults
        assertEquals(Boolean.FALSE,
                BeanUtil.propertyDefaultValue(tf.constructType(Boolean.TYPE), true));
        assertEquals(Integer.valueOf(0),
                BeanUtil.propertyDefaultValue(tf.constructType(Integer.TYPE), true));
        assertEquals(Double.valueOf(0.0),
                BeanUtil.propertyDefaultValue(tf.constructType(Double.TYPE), true));

        // POJOs have no real default
        assertNull(BeanUtil.propertyDefaultValue(tf.constructType(getClass()), true));
    }

    @Test
    public void testGetDefaultValueWrappers()
    {
        final TypeFactory tf = defaultTypeFactory();

        // [databind#5570]: primitive wrappers (Integer, Boolean, etc.) may
        // default to null
        assertNull(BeanUtil.propertyDefaultValue(tf.constructType(Boolean.class), true));
        assertNull(BeanUtil.propertyDefaultValue(tf.constructType(Integer.class), true));
        assertNull(BeanUtil.propertyDefaultValue(tf.constructType(Double.class), true));

        //... or not
        assertEquals(Boolean.FALSE,
                BeanUtil.propertyDefaultValue(tf.constructType(Boolean.class), false));
        assertEquals(Integer.valueOf(0),
                BeanUtil.propertyDefaultValue(tf.constructType(Integer.class), false));
        assertEquals(Double.valueOf(0.0),
                BeanUtil.propertyDefaultValue(tf.constructType(Double.class), false));
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testGetDefaultValueDeprecated()
    {
        final TypeFactory tf = defaultTypeFactory();

        assertNull(BeanUtil.getDefaultValue(tf.constructType(Boolean.class)));
        assertNull(BeanUtil.getDefaultValue(tf.constructType(Integer.class)));
        assertNull(BeanUtil.getDefaultValue(tf.constructType(Double.class)));
    }
    
    @Test
    public void testGetDefaultValuesForJDKTypes()
    {
        final TypeFactory tf = defaultTypeFactory();

        // java.util.Date
        Object result = BeanUtil.propertyDefaultValue(tf.constructType(Date.class), true);
        assertNotNull(result);
        assertInstanceOf(Date.class, result);
        assertEquals(0L, ((Date) result).getTime());

        // java.util.Calendar
        result = BeanUtil.propertyDefaultValue(tf.constructType(Calendar.class), true);
        assertNotNull(result);
        assertInstanceOf(Calendar.class, result);
        assertEquals(0L, ((Calendar) result).getTimeInMillis());

        // java.util.GregorianCalendar
        result = BeanUtil.propertyDefaultValue(tf.constructType(GregorianCalendar.class), true);
        assertNotNull(result);
        assertInstanceOf(Calendar.class, result);
        assertEquals(0L, ((Calendar) result).getTimeInMillis());

        // java.util.UUID
        result = BeanUtil.propertyDefaultValue(tf.constructType(UUID.class), true);
        assertEquals(JsonInclude.Include.NON_EMPTY, result);
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
