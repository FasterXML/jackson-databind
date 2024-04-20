package tools.jackson.databind.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BeanUtilTest extends DatabindTestUtil
{
    @Test
    public void testGetDefaultValue()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
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
}
