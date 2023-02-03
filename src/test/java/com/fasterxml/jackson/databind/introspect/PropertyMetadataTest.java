package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.*;

public class PropertyMetadataTest extends BaseMapTest
{
    public void testPropertyName()
    {
        PropertyName name = PropertyName.NO_NAME;

        assertFalse(name.hasSimpleName());
        assertFalse(name.hasNamespace());
        assertSame(name, name.internSimpleName());
        assertSame(name, name.withSimpleName(null));
        assertSame(name, name.withSimpleName(""));
        assertSame(name, name.withNamespace(null));
        assertEquals("", name.toString());
        assertTrue(name.isEmpty());
        assertFalse(name.hasSimpleName("foo"));
        // just to trigger it, ensure to exception
        name.hashCode();

        PropertyName newName = name.withNamespace("");
        assertNotSame(name, newName);
        assertTrue(name.equals(name));
        assertFalse(name.equals(newName));
        assertFalse(newName.equals(name));

        name = name.withSimpleName("foo");
        assertEquals("foo", name.toString());
        assertTrue(name.hasSimpleName("foo"));
        assertFalse(name.isEmpty());
        newName = name.withNamespace("ns");
        assertEquals("{ns}foo", newName.toString());
        assertFalse(newName.equals(name));
        assertFalse(name.equals(newName));

        // just to trigger it, ensure to exception
        name.hashCode();
    }

    public void testPropertyMetadata()
    {
        PropertyMetadata md = PropertyMetadata.STD_OPTIONAL;
        assertNull(md.getValueNulls());
        assertNull(md.getContentNulls());
        assertNull(md.getDefaultValue());
        assertEquals(Boolean.FALSE, md.getRequired());

        md = md.withNulls(Nulls.AS_EMPTY,
                Nulls.FAIL);
        assertEquals(Nulls.AS_EMPTY, md.getValueNulls());
        assertEquals(Nulls.FAIL, md.getContentNulls());

        assertFalse(md.hasDefaultValue());
        assertSame(md, md.withDefaultValue(null));
        assertSame(md, md.withDefaultValue(""));
        md = md.withDefaultValue("foo");
        assertEquals("foo", md.getDefaultValue());
        assertTrue(md.hasDefaultValue());
        assertSame(md, md.withDefaultValue("foo"));
        md = md.withDefaultValue(null);
        assertFalse(md.hasDefaultValue());
        assertNull(md.getDefaultValue());

        md = md.withRequired(null);
        assertNull(md.getRequired());
        assertFalse(md.isRequired());
        md = md.withRequired(Boolean.TRUE);
        assertTrue(md.isRequired());
        assertSame(md, md.withRequired(Boolean.TRUE));
        md = md.withRequired(null);
        assertNull(md.getRequired());
        assertFalse(md.isRequired());

        assertFalse(md.hasIndex());
        md = md.withIndex(Integer.valueOf(3));
        assertTrue(md.hasIndex());
        assertEquals(Integer.valueOf(3), md.getIndex());
    }
}
